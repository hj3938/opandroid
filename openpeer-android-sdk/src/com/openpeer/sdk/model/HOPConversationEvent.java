/**
 * Copyright (c) 2014, SMB Phone Inc. / Hookflash Inc.
 * All rights reserved.
 * <p/>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * <p/>
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * <p/>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * <p/>
 * The views and conclusions contained in the software and documentation are those
 * of the authors and should not be interpreted as representing official policies,
 * either expressed or implied, of the FreeBSD Project.
 */
package com.openpeer.sdk.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 */
public class HOPConversationEvent<T> {
    String conversationId;
    private EventTypes eventType;
    private T event;
    private long mId;
    private long mTime;
    long cbcId;

    public long getTime() {
        return mTime;
    }

    public long getCbcId() {
        return cbcId;
    }

    public void setCbcId(long cbcId) {
        this.cbcId = cbcId;
    }

    public static HOPConversationEvent newContactsChangeEvent(String conversationId,
                                                              long cbcId, long[] added,
                                                              long[] removed) {
        HOPConversationEvent<ContactsChange> changeEvent = new HOPConversationEvent<ContactsChange>
            (conversationId,
             EventTypes.ContactsChange,
             null,
             cbcId,
             System.currentTimeMillis());
        ContactsChange change = new ContactsChange();
        if (added != null && added.length > 0) {
            change.added = added;
        }
        if (removed != null && removed.length > 0) {
            change.removed = removed;
        }
        changeEvent.event = change;
        return changeEvent;
    }

    public static ContactsChange contactsChangeFromJson(String jsonBlob) {
        try {
            JSONObject jsonObject = new JSONObject(jsonBlob);
            JSONArray addedArray = jsonObject.optJSONArray(ContactsChange.KEY_ADDED);
            JSONArray removedArray = jsonObject.optJSONArray(ContactsChange.KEY_REMOVED);
            ContactsChange event = new ContactsChange();
            if (addedArray != null && addedArray.length() > 0) {
                long[] added = new long[addedArray.length()];
                for (int i = 0; i < added.length; i++) {
                    added[i] = addedArray.getLong(i);
                }
                event.added = added;
            }
            if (removedArray != null && removedArray.length() > 0) {
                long[] removed = new long[removedArray.length()];
                for (int i = 0; i < removed.length; i++) {
                    removed[i] = removedArray.getLong(i);
                }
                event.removed = removed;
            }
            return event;
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @param conversation TODO
     * @param type
     * @param event        TODO
     */
    public HOPConversationEvent(HOPConversation conversation, EventTypes type, T event) {
        this(conversation.getConversationId(), type, event, conversation.getCurrentCbcId(),
             System.currentTimeMillis());
    }

    public HOPConversationEvent(String conversationId,
                                EventTypes event,
                                T description,
                                long cbcId,
                                long time) {
        super();
        this.conversationId = conversationId;
        this.eventType = event;
        this.event = description;
        this.cbcId = cbcId;
        this.mTime = time;

    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversation(String conversationId) {
        this.conversationId = conversationId;
    }

    public void setEvent(T description) {
        this.event = description;
    }

    public void setId(long id) {
        this.mId = id;
    }

    public enum EventTypes {
        ContactsChange,
        TopicChange
    }

    public EventTypes getEventType() {
        return eventType;
    }

    public void setEventType(EventTypes eventType) {
        this.eventType = eventType;
    }

    public String getContentString() {
        return event.toString();
    }

    /**
     * @return
     */
    public long getId() {
        // TODO Auto-generated method stub
        return mId;
    }

    public static class ContactsChange {
        public static final String KEY_ADDED = "added";
        public static final String KEY_REMOVED = "removed";
        long added[];
        long removed[];

        public ContactsChange() {
        }

        public ContactsChange(long[] added, long[] removed) {
            this.added = added;
            this.removed = removed;
        }

        public long[] getAdded() {
            return added;
        }

        public long[] getRemoved() {
            return removed;
        }

        public String toString() {
            try {
                JSONObject object = new JSONObject();
                if (added != null) {
                    JSONArray array = new JSONArray();
                    for (long value : added) {
                        array.put(value);
                    }
                    object.put(KEY_ADDED, array);
                }
                if (removed != null) {
                    JSONArray array = new JSONArray();
                    for (long value : removed) {
                        array.put(value);
                    }
                    object.put(KEY_REMOVED, array);
                }
                return object.toString();
            } catch(JSONException e) {

            }
            return null;
        }
    }


}
