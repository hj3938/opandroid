/*
 * ******************************************************************************
 *  *
 *  *  Copyright (c) 2014 , Hookflash Inc.
 *  *  All rights reserved.
 *  *
 *  *  Redistribution and use in source and binary forms, with or without
 *  *  modification, are permitted provided that the following conditions are met:
 *  *
 *  *  1. Redistributions of source code must retain the above copyright notice, this
 *  *  list of conditions and the following disclaimer.
 *  *  2. Redistributions in binary form must reproduce the above copyright notice,
 *  *  this list of conditions and the following disclaimer in the documentation
 *  *  and/or other materials provided with the distribution.
 *  *
 *  *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 *  *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  *
 *  *  The views and conclusions contained in the software and documentation are those
 *  *  of the authors and should not be interpreted as representing official policies,
 *  *  either expressed or implied, of the FreeBSD Project.
 *  ******************************************************************************
 */

package com.openpeer.sample.push.parsepush;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.openpeer.javaapi.MessageDeliveryStates;
import com.openpeer.javaapi.OPMessage;
import com.openpeer.sample.OPApplication;
import com.openpeer.sample.push.PushExtra;
import com.openpeer.sdk.app.OPDataManager;
import com.openpeer.sdk.model.OPConversation;
import com.openpeer.sdk.model.OPUser;
import com.openpeer.sdk.model.PushServiceInterface;
import com.openpeer.sdk.model.SystemMessage;
import com.parse.FunctionCallback;
import com.parse.Parse;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.SaveCallback;
import com.parse.SendCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

public class PFPushService implements PushServiceInterface {
    public static final String TAG = PFPushService.class.getSimpleName();
    public static final String KEY_PARSE_APP_ID = "parseAppId";
    public static final String KEY_PARSE_CLIENT_KEY = "parseClientKey";
    static final String KEY_PEER_URI = "peerUri";
    static final String KEY_OS_VERSION = "osVersion";
    private static PFPushService instance;
    static long PUSH_EXPIRATION_DEFAULT = 30 * 24 * 60 * 60;

    public boolean isInitialized() {
        return mInitialized;
    }

    private boolean mInitialized;

    public static PFPushService getInstance() throws RuntimeException {
        if (instance == null) {

            instance = new PFPushService();
            String parseAppId = OPApplication.getMetaInfo(KEY_PARSE_APP_ID);
            String parseClientKey = OPApplication.getMetaInfo(KEY_PARSE_CLIENT_KEY);
            if (TextUtils.isEmpty(parseAppId)) {
                throw new RuntimeException("Parse application id is not defined");
            } else if (TextUtils.isEmpty(parseClientKey)) {
                throw new RuntimeException("Parse client key is not defined");
            }

            Parse.initialize(OPApplication.getInstance(),
                             parseAppId,
                             parseClientKey);
        }
        return instance;
    }

    public boolean init() {
        OPUser currentUser = OPDataManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            ParseInstallation.getCurrentInstallation().put(KEY_PEER_URI, currentUser.getPeerUri());
            ParseInstallation.getCurrentInstallation().put(KEY_OS_VERSION,
                                                           "" + Build.VERSION.SDK_INT);
            ParseInstallation.getCurrentInstallation().saveInBackground(new SaveCallback() {
                @Override
                public void done(ParseException e) {
                    mInitialized = true;
                    PFPushReceiver.downloadMessages();
                }
            });

            return true;
        }
        return false;
    }

    private PFPushService() {
    }

    @Override
    public void onConversationThreadPushMessage(final OPConversation conversation,
                                                final OPMessage message, final OPUser contact) {
        if (!mInitialized) {
            return;
        }
        List<OPUser> participants = conversation.getParticipants();
        String peerURIs = "";

        String peerUris[] = new String[participants.size() - 1];
        //We only put the peerURIs other than myself and the recipient
        if (participants.size() > 1) {
            int i = 0;
            for (OPUser user : participants) {
                if (!user.equals(contact)) {
                    peerUris[i] = user.getPeerUri();
                    i++;
                }
            }
            peerURIs = TextUtils.join(",", peerUris);
        }

//        PushExtra pushExtra = new PushExtra
//            (OPDataManager.getInstance().getCurrentUser().getPeerUri(),
//             OPDataManager.getInstance().getCurrentUser().getName(),
//             peerURIs,
//             message.getMessageType(),
//             message.getMessageId(),
//             message.getMessageId(),
//             conversation.getType().toString(),
//             conversation.getConversationId(),
//             OPDataManager.getInstance().getSharedAccount().getLocationID(),
//             message.getTime().toMillis(false) / 1000 + "");

        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put(PFPushMessage.KEY_TO, contact.getPeerUri());
        params.put(PFPushMessage.KEY_PEER_URI, OPDataManager.getInstance().getCurrentUser()
            .getPeerUri());
        params.put(PFPushMessage.KEY_SENDER_NAME, OPDataManager.getInstance().getCurrentUser()
            .getName());
        params.put(PFPushMessage.KEY_PEER_URIS, peerURIs);
        params.put(PFPushMessage.KEY_MESSAGE_ID, message.getMessageId());
        params.put(PFPushMessage.KEY_CONVERSATION_TYPE, conversation.getType().name());
        params.put(PFPushMessage.KEY_CONVERSATION_ID, conversation.getConversationId());
        params.put(PFPushMessage.KEY_LOCATION, OPDataManager.getInstance().getSharedAccount()
            .getLocationID());
        params.put(PFPushMessage.KEY_DATE, message.getTime().toMillis(false) / 1000);
        String messageType = message.getMessageType();
        params.put(PFPushMessage.KEY_MESSAGE_TYPE, messageType);

        switch (messageType){
        case OPMessage.TYPE_TEXT:{
            params.put(PFPushMessage.KEY_ALERT, message.getMessage());
        }
        break;
        case OPMessage.TYPE_JSON_SYSTEM_MESSAGE:{
            try {
                JSONObject jsonObject = new JSONObject(message.getMessage().replace("\"$id\"",
                                                                                    "\"id\""));
                JSONObject systemObject = jsonObject.getJSONObject(SystemMessage.KEY_ROOT);
                params.put("system", systemObject);
//                if (systemObject.has(SystemMessage.KEY_CALL_STATUS)) {
//                    systemObject=systemObject.getJSONObject(SystemMessage.KEY_CALL_STATUS);
//                    params.put(PFPushMessage.KEY_MESSAGE_TYPE,
//                               PFPushMessage.MESSAGE_TYPE_CALL_STATE);
//                    params.put(PFPushMessage.KEY_CALL_STATE,
//                               systemObject.getString(SystemMessage.KEY_CALL_STATUS_STATUS));
//                    params.put(PFPushMessage.KEY_CALL_MEDIA_TYPE,
//                               systemObject.getString(SystemMessage.KEY_CALL_STATUS_MEDIA_TYPE));
//                    params.put(PFPushMessage.KEY_CALL_ID,
//                               systemObject.getString(SystemMessage.KEY_CALL_STATUS_CALL_ID));
//                } else if (systemObject.has(SystemMessage.KEY_CONTACTS_REMOVED)) {
//                    params.put(PFPushMessage.KEY_MESSAGE_TYPE,
//                               PFPushMessage.MESSAGE_TYPE_CONTACTS_REMOVED);
//                    // TODO: add others
//                }

            } catch(JSONException e) {
                e.printStackTrace();
            }
        }
        break;
        }

        ParseCloud.callFunctionInBackground(
            "sendPushToUser",
            params,
            new FunctionCallback<Object>() {
                public void done(Object success, ParseException e) {
                    if (e == null) {
                        Log.d("ParsePush", "success " + success);
                        // Push sent successfully
                        OPDataManager.getInstance()
                            .updateMessageDeliveryStatus(
                                message.getMessageId(),
                                conversation.getConversationId(),
                                MessageDeliveryStates.MessageDeliveryState_Sent);

                    } else {
                        //TODO: proper error handling
                        e.printStackTrace();
                    }
                }
            });
    }

    public void onSignout() {
        ParseInstallation.getCurrentInstallation().put(KEY_PEER_URI, "");
        ParseInstallation.getCurrentInstallation().saveInBackground();
    }
}