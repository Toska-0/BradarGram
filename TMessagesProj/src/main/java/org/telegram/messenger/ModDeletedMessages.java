package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.collection.LongSparseArray;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Хранит список сообщений, удалённых собеседником, но сохранённых у нас
 * (при включённой функции «Сохранять удалённые сообщения»).
 * Сами сообщения остаются в обычной базе Telegram, здесь только пометки.
 */
public class ModDeletedMessages {

    private static final String PREFS = "mod_deleted_messages";
    private static final String KEY = "ids";
    private static final int MAX_ENTRIES = 50000;

    private static Set<String> set;

    private static synchronized Set<String> set() {
        if (set == null) {
            SharedPreferences p = ApplicationLoader.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            set = new HashSet<>(p.getStringSet(KEY, new HashSet<>()));
        }
        return set;
    }

    private static String key(int account, long dialogKey, int id) {
        return account + "_" + dialogKey + "_" + id;
    }

    /** dialogKey: 0 для обычных чатов (id сообщений общие), -channelId для каналов и супергрупп. */
    public static void remember(int account, LongSparseArray<ArrayList<Integer>> deleted) {
        if (deleted == null) {
            return;
        }
        boolean changed = false;
        synchronized (ModDeletedMessages.class) {
            Set<String> s = set();
            for (int a = 0; a < deleted.size(); a++) {
                long dialogKey = deleted.keyAt(a);
                ArrayList<Integer> ids = deleted.valueAt(a);
                if (ids == null) {
                    continue;
                }
                for (int i = 0; i < ids.size(); i++) {
                    changed |= s.add(key(account, dialogKey, ids.get(i)));
                }
            }
            if (changed) {
                if (s.size() > MAX_ENTRIES) {
                    s.clear();
                }
                ApplicationLoader.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                        .edit().putStringSet(KEY, new HashSet<>(s)).apply();
            }
        }
        if (changed) {
            AndroidUtilities.runOnUIThread(() ->
                    NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.modDeletedMessagesUpdated));
        }
    }

    public static void remember(int account, long channelId, ArrayList<Integer> ids) {
        LongSparseArray<ArrayList<Integer>> map = new LongSparseArray<>();
        map.put(channelId == 0 ? 0 : -channelId, ids);
        remember(account, map);
    }

    public static boolean isDeleted(MessageObject mo) {
        if (mo == null || mo.messageOwner == null) {
            return false;
        }
        long dialogKey = 0;
        if (mo.messageOwner.peer_id != null && mo.messageOwner.peer_id.channel_id != 0) {
            dialogKey = -mo.messageOwner.peer_id.channel_id;
        }
        synchronized (ModDeletedMessages.class) {
            return set().contains(key(mo.currentAccount, dialogKey, mo.getId()));
        }
    }
}
