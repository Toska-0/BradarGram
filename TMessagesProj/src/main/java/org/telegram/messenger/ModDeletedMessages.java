package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.collection.LongSparseArray;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;

/**
 * Хранит список сообщений, удалённых собеседником, но сохранённых у нас
 * (при включённой функции «Сохранять удалённые сообщения»).
 * Сами сообщения остаются в обычной базе Telegram, здесь только пометки.
 */
public class ModDeletedMessages {

    private static final String PREFS = "mod_deleted_messages";
    private static final String KEY = "ids";
    private static final int MAX_ENTRIES = 50000;

    /** Порядок вставки: от старых к новым. Персистится от новых к старым. */
    private static LinkedHashSet<String> cache;

    private static synchronized LinkedHashSet<String> set() {
        if (cache == null) {
            cache = new LinkedHashSet<>();
            SharedPreferences p = ApplicationLoader.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String raw = p.getString(KEY, "");
            if (!raw.isEmpty()) {
                for (String line : raw.split("\n")) {
                    if (!line.isEmpty()) {
                        cache.add(line);
                    }
                }
            }
        }
        return cache;
    }

    private static String key(int account, long dialogKey, int id) {
        return account + "_" + dialogKey + "_" + id;
    }

    /**
     * dialogKey: 0 для обычных чатов (id сообщений общие на аккаунт),
     * -channelId для каналов и супергрупп.
     */
    public static void remember(int account, LongSparseArray<ArrayList<Integer>> deleted) {
        if (deleted == null) {
            return;
        }
        boolean changed = false;
        synchronized (ModDeletedMessages.class) {
            LinkedHashSet<String> s = set();
            for (int a = 0; a < deleted.size(); a++) {
                long dialogKey = deleted.keyAt(a);
                if (dialogKey > 0) {
                    // Личные чаты и обычные группы используют общую последовательность id
                    // на аккаунте, поэтому храним их под общим ключом 0, как в isDeleted.
                    dialogKey = 0;
                }
                ArrayList<Integer> ids = deleted.valueAt(a);
                if (ids == null) {
                    continue;
                }
                for (int i = 0; i < ids.size(); i++) {
                    changed |= s.add(key(account, dialogKey, ids.get(i)));
                }
            }
            if (changed) {
                trimOldest(s);
                persist(s);
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

    /** Удаляем самые старые записи (начало порядка вставки), а не весь список разом. */
    private static void trimOldest(LinkedHashSet<String> s) {
        int excess = s.size() - MAX_ENTRIES;
        if (excess <= 0) {
            return;
        }
        Iterator<String> it = s.iterator();
        while (excess-- > 0 && it.hasNext()) {
            it.next();
            it.remove();
        }
    }

    private static void persist(LinkedHashSet<String> s) {
        ArrayList<String> ordered = new ArrayList<>(s);
        StringBuilder sb = new StringBuilder();
        for (int i = ordered.size() - 1; i >= 0; i--) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(ordered.get(i));
        }
        ApplicationLoader.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY, sb.toString()).apply();
    }
}
