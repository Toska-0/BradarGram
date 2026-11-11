package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.ModConfig;
import org.telegram.messenger.ModTheme;
import org.telegram.messenger.ModUtils;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.SimpleActionBarMenu;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.Locale;

/** Экран «Настройки мода»: категории и поиск по настройкам. */
public class ModSettingsActivity extends BaseFragment {

    // типы строк
    private static final int T_HEADER = 0;
    private static final int T_CHECK = 1;
    private static final int T_INFO = 2;
    private static final int T_VALUE = 3;
    private static final int T_BLUR_PREVIEW = 4;
    private static final int T_COLOR_PREVIEW = 5;

    // категории
    private static final int CAT_APPEARANCE = 0;
    private static final int CAT_PRIVACY = 1;
    private static final int CAT_MESSAGES = 2;
    private static final int CAT_PREMIUM = 3;
    private static final int CAT_ABOUT = 4;
    private static final int[] CAT_TITLES = {
            R.string.ModCatAppearance,
            R.string.ModCatPrivacy,
            R.string.ModCatMessages,
            R.string.ModCatPremium,
            R.string.ModCatAbout
    };

    private static final String KEY_PACK = "color_pack_item";
    private static final String KEY_VERSION = "version_item";

    /** Настройка, по которой работает поиск. */
    private static class Item {
        final String key;
        final int type;
        final int titleRes;
        final int category;
        final String keywords;

        Item(String key, int type, int titleRes, int category, String keywords) {
            this.key = key;
            this.type = type;
            this.titleRes = titleRes;
            this.category = category;
            this.keywords = keywords;
        }
    }

    private static final Item[] ITEMS = {
            new Item(ModConfig.KEY_NO_BLUR, T_CHECK, R.string.ModNoBlur, CAT_APPEARANCE, "blur размытие стекло glass liquid прозрачность"),
            new Item(KEY_PACK, T_VALUE, R.string.ModColorPack, CAT_APPEARANCE, "цвет цвета тема оформление пакет color theme palette accent"),
            new Item(ModConfig.KEY_NO_TYPING, T_CHECK, R.string.ModNoTyping, CAT_PRIVACY, "печатает typing статус ghost"),
            new Item(ModConfig.KEY_NO_READ, T_CHECK, R.string.ModNoRead, CAT_PRIVACY, "прочитано прочтение read receipts тихо невидимка"),
            new Item(ModConfig.KEY_CONFIRM_CALLS, T_CHECK, R.string.ModConfirmCalls, CAT_PRIVACY, "звонок звонки call confirm подтверждение"),
            new Item(ModConfig.KEY_SAVE_DELETED, T_CHECK, R.string.ModSaveDeleted, CAT_MESSAGES, "удалённые удаленные deleted сохранять корзина"),
            new Item(ModConfig.KEY_LOCAL_PREMIUM, T_CHECK, R.string.ModLocalPremium, CAT_PREMIUM, "премиум premium локальный подписка"),
            new Item(ModConfig.KEY_HIDE_PREMIUM, T_CHECK, R.string.ModHidePremium, CAT_PREMIUM, "премиум premium скрыть hide"),
            new Item(ModConfig.KEY_HIDE_STARS, T_CHECK, R.string.ModHideStars, CAT_PREMIUM, "звёзды звезды stars скрыть hide"),
            new Item(KEY_VERSION, T_VALUE, R.string.ModVersion, CAT_ABOUT, "версия version о моде about")
    };

    /** Строка списка. */
    private static class Row {
        final int type;
        final Item item;
        final int category;
        final int textRes;

        Row(int type, Item item, int category, int textRes) {
            this.type = type;
            this.item = item;
            this.category = category;
            this.textRes = textRes;
        }
    }

    private final ArrayList<Row> rows = new ArrayList<>();
    private String query = "";

    private RecyclerListView listView;
    private ListAdapter adapter;

    @Override
    protected boolean useSimpleActionBar() {
        return true;
    }

    @Override
    public View createView(Context context) {
        setHasOwnBackground(true);
        simpleActionBar.setBackButtonImage(R.drawable.ic_ab_back);
        simpleActionBar.setTitle(LocaleController.getString(R.string.ModSettings));
        simpleActionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        final SimpleActionBarMenu menu = simpleActionBar.createMenu();
        ActionBarMenuItem searchItem = menu.addItem(0, R.drawable.outline_header_search).setIsSearchField(true).setActionBarMenuItemSearchListener(new ActionBarMenuItem.ActionBarMenuItemSearchListener() {
            @Override
            public void onSearchCollapse() {
                query = "";
                rebuildRows();
            }

            @Override
            public void onSearchExpand() {
                query = "";
            }

            @Override
            public void onTextChanged(EditText editText) {
                query = editText.getText().toString();
                rebuildRows();
            }
        });
        searchItem.setSearchFieldHint(LocaleController.getString(R.string.ModSearchHint));

        FrameLayout frameLayout = new FrameLayout(context);
        fragmentView = frameLayout;

        rebuildRows();

        listView = new RecyclerListView(context);
        listView.setSectionsDrawBackground(true);
        listView.setSections();
        listView.setLayoutManager(new LinearLayoutManager(context));
        listView.setVerticalScrollBarEnabled(false);
        listView.setAdapter(adapter = new ListAdapter(context));
        listView.setOnItemClickListener((view, position) -> onRowClicked(view, position));
        frameLayout.addView(listView, LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT,
                LayoutHelper.MATCH_PARENT
        ));
        return fragmentView;
    }

    private void rebuildRows() {
        rows.clear();
        final String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            addHeader(CAT_APPEARANCE);
            rows.add(new Row(T_CHECK, ITEMS[0], CAT_APPEARANCE, 0));
            rows.add(new Row(T_BLUR_PREVIEW, null, CAT_APPEARANCE, 0));
            rows.add(new Row(T_VALUE, ITEMS[1], CAT_APPEARANCE, 0));
            rows.add(new Row(T_COLOR_PREVIEW, null, CAT_APPEARANCE, 0));
            rows.add(new Row(T_INFO, null, CAT_APPEARANCE, R.string.ModAppearanceInfo));

            addHeader(CAT_PRIVACY);
            rows.add(new Row(T_CHECK, ITEMS[2], CAT_PRIVACY, 0));
            rows.add(new Row(T_CHECK, ITEMS[3], CAT_PRIVACY, 0));
            rows.add(new Row(T_CHECK, ITEMS[4], CAT_PRIVACY, 0));
            rows.add(new Row(T_INFO, null, CAT_PRIVACY, R.string.ModPrivacyInfo));

            addHeader(CAT_MESSAGES);
            rows.add(new Row(T_CHECK, ITEMS[5], CAT_MESSAGES, 0));
            rows.add(new Row(T_INFO, null, CAT_MESSAGES, R.string.ModMessagesInfo));

            addHeader(CAT_PREMIUM);
            rows.add(new Row(T_CHECK, ITEMS[6], CAT_PREMIUM, 0));
            rows.add(new Row(T_CHECK, ITEMS[7], CAT_PREMIUM, 0));
            rows.add(new Row(T_CHECK, ITEMS[8], CAT_PREMIUM, 0));
            rows.add(new Row(T_INFO, null, CAT_PREMIUM, R.string.ModPremiumInfo));

            addHeader(CAT_ABOUT);
            rows.add(new Row(T_VALUE, ITEMS[9], CAT_ABOUT, 0));
        } else {
            boolean any = false;
            for (int cat = 0; cat < CAT_TITLES.length; cat++) {
                boolean headerAdded = false;
                for (Item item : ITEMS) {
                    if (item.category != cat || !matches(item, q)) {
                        continue;
                    }
                    if (!headerAdded) {
                        addHeader(cat);
                        headerAdded = true;
                    }
                    rows.add(new Row(item.type, item, cat, 0));
                    any = true;
                }
            }
            if (!any) {
                rows.add(new Row(T_INFO, null, -1, R.string.ModNothingFound));
            }
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void addHeader(int category) {
        rows.add(new Row(T_HEADER, null, category, CAT_TITLES[category]));
    }

    private static boolean matches(Item item, String q) {
        String title = LocaleController.getString(item.titleRes).toLowerCase(Locale.ROOT);
        String cat = LocaleController.getString(CAT_TITLES[item.category]).toLowerCase(Locale.ROOT);
        return title.contains(q) || cat.contains(q) || item.keywords.contains(q);
    }

    private static String packName(int pack) {
        if (pack < 0 || pack >= ModTheme.PACK_NAMES.length) {
            pack = 0;
        }
        return LocaleController.getString(ModTheme.PACK_NAMES[pack]);
    }

    private void onRowClicked(View view, int position) {
        if (position < 0 || position >= rows.size()) {
            return;
        }
        Row row = rows.get(position);
        if (row.item == null) {
            return;
        }
        if (KEY_PACK.equals(row.item.key)) {
            showPackDialog();
            return;
        }
        if (row.type == T_CHECK && view instanceof TextCheckCell) {
            boolean value = ModConfig.toggle(row.item.key);
            ((TextCheckCell) view).setChecked(value);
            if (ModConfig.KEY_NO_BLUR.equals(row.item.key)) {
                refreshPreviews();
                showRestartDialog();
            } else if (ModConfig.KEY_LOCAL_PREMIUM.equals(row.item.key)) {
                showRestartDialog();
            }
        }
    }

    private void refreshPreviews() {
        if (adapter == null) {
            return;
        }
        for (int i = 0; i < rows.size(); i++) {
            int type = rows.get(i).type;
            if (type == T_BLUR_PREVIEW || type == T_COLOR_PREVIEW) {
                adapter.notifyItemChanged(i);
            }
        }
    }

    private void showPackDialog() {
        if (getParentActivity() == null) {
            return;
        }
        CharSequence[] items = new CharSequence[ModTheme.PACK_NAMES.length];
        for (int i = 0; i < items.length; i++) {
            items[i] = packName(i);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString(R.string.ModColorPack));
        builder.setItems(items, (dialog, which) -> {
            ModConfig.setColorPack(which);
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
            ModTheme.applyLive();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showRestartDialog() {
        if (getParentActivity() == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString(R.string.ModRestartTitle));
        builder.setMessage(LocaleController.getString(R.string.ModRestartText));
        builder.setPositiveButton(LocaleController.getString(R.string.ModRestartNow), (dialog, which) -> ModUtils.restartApp());
        builder.setNegativeButton(LocaleController.getString(R.string.ModRestartLater), null);
        showDialog(builder.create());
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {
        private final Context context;

        private ListAdapter(Context context) {
            this.context = context;
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }

        @Override
        public int getItemViewType(int position) {
            return rows.get(position).type;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int position = holder.getAdapterPosition();
            if (position < 0 || position >= rows.size()) {
                return false;
            }
            Row row = rows.get(position);
            return row.type == T_CHECK || (row.type == T_VALUE && row.item != null && KEY_PACK.equals(row.item.key));
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case T_HEADER:
                    view = new HeaderCell(context);
                    break;
                case T_CHECK:
                    view = new TextCheckCell(context);
                    break;
                case T_VALUE:
                    view = new TextSettingsCell(context);
                    break;
                case T_BLUR_PREVIEW:
                    view = new ModBlurPreviewView(context);
                    break;
                case T_COLOR_PREVIEW:
                    view = new ModColorPreviewView(context);
                    break;
                default:
                    view = new TextInfoPrivacyCell(context);
                    break;
            }
            view.setLayoutParams(new RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT
            ));
            return new RecyclerListView.Holder(view);
        }

        private boolean hasDividerAfter(int position) {
            if (position + 1 >= rows.size()) {
                return false;
            }
            int next = rows.get(position + 1).type;
            return next == T_CHECK || next == T_VALUE;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Row row = rows.get(position);
            switch (row.type) {
                case T_HEADER: {
                    ((HeaderCell) holder.itemView).setText(LocaleController.getString(row.textRes));
                    break;
                }
                case T_CHECK: {
                    TextCheckCell cell = (TextCheckCell) holder.itemView;
                    cell.setTextAndCheck(LocaleController.getString(row.item.titleRes), ModConfig.get(row.item.key), hasDividerAfter(position));
                    break;
                }
                case T_VALUE: {
                    TextSettingsCell cell = (TextSettingsCell) holder.itemView;
                    String value = KEY_PACK.equals(row.item.key) ? packName(ModConfig.getColorPack()) : ModConfig.MOD_VERSION;
                    cell.setTextAndValue(LocaleController.getString(row.item.titleRes), value, hasDividerAfter(position));
                    break;
                }
                case T_BLUR_PREVIEW: {
                    holder.itemView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    ((ModBlurPreviewView) holder.itemView).setBlurDisabled(ModConfig.isNoBlur());
                    break;
                }
                case T_COLOR_PREVIEW: {
                    holder.itemView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    ((ModColorPreviewView) holder.itemView).setPack(ModConfig.getColorPack());
                    break;
                }
                default: {
                    ((TextInfoPrivacyCell) holder.itemView).setText(LocaleController.getString(row.textRes));
                    break;
                }
            }
        }
    }

    @Override
    protected RecyclerListView getListViewForSimpleGlass() {
        return listView;
    }

    @Override
    public boolean isSupportEdgeToEdge() {
        return true;
    }
}
