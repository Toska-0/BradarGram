package org.telegram.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FerrumConfig;
import org.telegram.messenger.FerrumAntivirus;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.ModConfig;
import org.telegram.messenger.ModTheme;
import org.telegram.messenger.ModUtils;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**
 * Ferrum settings rendered with Telegram's native settings cells and theme.
 * All preference values remain stored by FerrumConfig / ModConfig.
 */
public class FerrumSettingsActivity extends BaseFragment {
    private static final int REQUEST_BANNER = 7101;
    private static final int REQUEST_SCAN = 7102;
    private static final int REQUEST_EXPORT = 7103;
    private static final int REQUEST_IMPORT = 7104;

    private static final int HEADER = 0;
    private static final int CHECK = 1;
    private static final int VALUE = 2;
    private static final int INFO = 3;
    private static final int PREVIEW_BLUR = 4;
    private static final int PREVIEW_COLOR = 5;
    private static final int PREVIEW_PROFILE = 6;

    private static final int ACCENT = 1, BANNER_PICK = 2, BANNER_REMOVE = 3,
            BANNER_CONFIGURE = 4,
            AVATAR_RING = 7, AVATAR_WIDTH = 8, AVATAR_COLOR = 9, AVATAR_STYLE = 10,
            AV_ENGINE = 11, AI_PROVIDER = 12, AI_KEY = 13, VT_KEY = 14,
            EXPORT = 15, IMPORT = 16, SCAN = 17, COLOR_PACK = 18, UI_STYLE = 19;
    private static final int NO_TYPING = 30, NO_READ = 31, SAVE_DELETED = 32,
            CONFIRM_CALLS = 33, LOCAL_PREMIUM = 34, HIDE_PREMIUM = 35,
            HIDE_STARS = 36, NO_BLUR = 37;

    private static class Row {
        int type, id;
        String title, value;
        boolean checked;
        Row(int type, int id, String title, String value, boolean checked) {
            this.type = type; this.id = id; this.title = title; this.value = value; this.checked = checked;
        }
    }

    private final ArrayList<Row> rows = new ArrayList<>();
    private RecyclerListView listView;
    private ListAdapter adapter;
    private String searchQuery;

    @Override
    protected boolean useSimpleActionBar() { return true; }

    @Override
    public View createView(Context context) {
        setHasOwnBackground(true);
        simpleActionBar.setBackButtonImage(R.drawable.ic_ab_back);
        simpleActionBar.setTitle("Ferrum");
        simpleActionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) { if (id == -1) finishFragment(); }
        });
        ActionBarMenuItem searchItem = simpleActionBar.createMenu().addItem(1, R.drawable.outline_search_1_24);
        searchItem.setIsSearchField(true);
        searchItem.setActionBarMenuItemSearchListener(new ActionBarMenuItem.ActionBarMenuItemSearchListener() {
            @Override
            public void onTextChanged(EditText editText) {
                String q = editText.getText() == null ? "" : editText.getText().toString().trim().toLowerCase();
                searchQuery = q.isEmpty() ? null : q;
                applySearchFilter();
            }
            @Override
            public void onSearchCollapse() {
                searchQuery = null;
                applySearchFilter();
            }
        });

        FrameLayout frame = new FrameLayout(context);
        fragmentView = frame;
        frame.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        rebuildRows(context);
        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context));
        listView.setVerticalScrollBarEnabled(false);
        listView.setAdapter(adapter = new ListAdapter(context));
        listView.setOnItemClickListener((view, position) -> {
            if (position >= 0 && position < rows.size()) onRowClick(view, rows.get(position));
        });
        frame.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        return fragmentView;
    }

    private void header(String title) { if (isFiltering()) return; rows.add(new Row(HEADER, 0, title, null, false)); }
    private void check(int id, String title, boolean checked) { if (!matches(title)) return; rows.add(new Row(CHECK, id, title, null, checked)); }
    private void value(int id, String title, String value) { if (!matches(title)) return; rows.add(new Row(VALUE, id, title, value, false)); }
    private void info(String text) { if (!matches(text)) return; rows.add(new Row(INFO, 0, text, null, false)); }
    private boolean isFiltering() { return searchQuery != null && !searchQuery.isEmpty(); }
    private boolean matches(String text) { return !isFiltering() || (text != null && text.toLowerCase().contains(searchQuery)); }
    private void applySearchFilter() {
        Context c = getParentActivity();
        if (c == null) return;
        rebuildRows(c);
        if (adapter != null) adapter.notifyDataSetChanged();
        if (listView != null) listView.scrollToPosition(0);
    }

    private void rebuildRows(Context c) {
        rows.clear();
        header(LocaleController.getString(R.string.FerrumSectionAppearance));
        value(ACCENT, LocaleController.getString(R.string.FerrumAccentColor), LocaleController.getString(R.string.FerrumChange));
        check(NO_BLUR, LocaleController.getString(R.string.FerrumNoBlur), ModConfig.isNoBlur());
        if (!isFiltering()) rows.add(new Row(PREVIEW_BLUR, 0, null, null, false));
        value(COLOR_PACK, LocaleController.getString(R.string.FerrumColorPalette), packName(ModConfig.getColorPack()));
        if (!isFiltering()) rows.add(new Row(PREVIEW_COLOR, 0, null, null, false));
        value(UI_STYLE, LocaleController.getString(R.string.FerrumUiStyle), uiPresetName(FerrumConfig.getUiPreset(c)));
        info(LocaleController.getString(R.string.FerrumBlurRestartInfo));

        header(LocaleController.getString(R.string.FerrumSectionBanner));
        value(BANNER_PICK, LocaleController.getString(R.string.FerrumBannerPick), LocaleController.getString(R.string.FerrumGallery));
        if (FerrumConfig.getProfileBannerUri(c) != null) {
            value(BANNER_CONFIGURE, LocaleController.getString(R.string.FerrumBannerConfigure), null);
            value(BANNER_REMOVE, LocaleController.getString(R.string.FerrumBannerRemove), null);
        }

        header(LocaleController.getString(R.string.FerrumSectionAvatar));
        if (!isFiltering()) rows.add(new Row(PREVIEW_PROFILE, 0, null, null, false));
        check(AVATAR_RING, LocaleController.getString(R.string.FerrumAvatarRing), FerrumConfig.isAvatarRingEnabled(c));
        value(AVATAR_WIDTH, LocaleController.getString(R.string.FerrumRingWidth), String.valueOf(FerrumConfig.getAvatarRingWidth(c)));
        value(AVATAR_COLOR, LocaleController.getString(R.string.FerrumRingColor), LocaleController.getString(R.string.FerrumChange));
        value(AVATAR_STYLE, LocaleController.getString(R.string.FerrumRingStyle), ringStyle(FerrumConfig.getAvatarRingStyle(c)));

        header(LocaleController.getString(R.string.FerrumSectionSecurity));
        check(40, LocaleController.getString(R.string.FerrumShowFileCheck), FerrumConfig.isAntivirusEnabled(c));
        if (FerrumConfig.isAntivirusEnabled(c)) {
            value(AV_ENGINE, LocaleController.getString(R.string.FerrumAvEngine), FerrumConfig.getAntivirusEngine(c) == FerrumConfig.ANTIVIRUS_ENGINE_VIRUSTOTAL ? "VirusTotal" : LocaleController.getString(R.string.FerrumEngineAi));
            value(AI_PROVIDER, LocaleController.getString(R.string.FerrumAiProvider), FerrumConfig.getAiProvider(c));
            value(AI_KEY, LocaleController.getString(R.string.FerrumAiKey), masked(FerrumConfig.getAiApiKey(c)));
            value(VT_KEY, LocaleController.getString(R.string.FerrumVtKey), masked(FerrumConfig.getVirusTotalApiKey(c)));
            value(SCAN, LocaleController.getString(R.string.FerrumScanFile), LocaleController.getString(R.string.FerrumChooseFile));
            info(LocaleController.getString(R.string.FerrumAvInfo));
        }

        header(LocaleController.getString(R.string.FerrumSectionPrivacy));
        check(NO_TYPING, LocaleController.getString(R.string.FerrumNoTyping), ModConfig.get(ModConfig.KEY_NO_TYPING));
        check(NO_READ, LocaleController.getString(R.string.FerrumNoRead), ModConfig.get(ModConfig.KEY_NO_READ));
        check(SAVE_DELETED, LocaleController.getString(R.string.FerrumSaveDeleted), ModConfig.get(ModConfig.KEY_SAVE_DELETED));
        check(CONFIRM_CALLS, LocaleController.getString(R.string.FerrumConfirmCalls), ModConfig.get(ModConfig.KEY_CONFIRM_CALLS));

        header(LocaleController.getString(R.string.FerrumSectionTelegram));
        check(LOCAL_PREMIUM, LocaleController.getString(R.string.FerrumLocalPremium), ModConfig.get(ModConfig.KEY_LOCAL_PREMIUM));
        check(HIDE_PREMIUM, LocaleController.getString(R.string.FerrumHidePremium), ModConfig.get(ModConfig.KEY_HIDE_PREMIUM));
        check(HIDE_STARS, LocaleController.getString(R.string.FerrumHideStars), ModConfig.get(ModConfig.KEY_HIDE_STARS));
        info(LocaleController.getString(R.string.FerrumPremiumInfo));

        header(LocaleController.getString(R.string.FerrumSectionSync));
        value(EXPORT, LocaleController.getString(R.string.FerrumExport), "JSON");
        value(IMPORT, LocaleController.getString(R.string.FerrumImport), "JSON");
        info(LocaleController.getString(R.string.FerrumSyncInfo));

        header(LocaleController.getString(R.string.FerrumSectionAbout));
        value(0, LocaleController.getString(R.string.FerrumVersion), ModConfig.MOD_VERSION);
    }

    private String masked(String key) { return key == null || key.isEmpty() ? LocaleController.getString(R.string.FerrumKeyNotSet) : "••••••••"; }
    private String packName(int pack) {
        if (pack < 0 || pack >= ModTheme.PACK_NAMES.length) pack = 0;
        return LocaleController.getString(ModTheme.PACK_NAMES[pack]);
    }
    private String uiPresetName(int preset) {
        if (preset == FerrumConfig.UI_PRESET_FULL_WIDTH) return LocaleController.getString(R.string.FerrumUiPresetFullWidth);
        if (preset == FerrumConfig.UI_PRESET_FLOATING) return LocaleController.getString(R.string.FerrumUiPresetFloating);
        return LocaleController.getString(R.string.FerrumUiPresetDefault);
    }
    private String ringStyle(int style) {
        String[] names = {LocaleController.getString(R.string.FerrumRingSolid), LocaleController.getString(R.string.FerrumRingGradient), LocaleController.getString(R.string.FerrumRingDashed)};
        return style >= 0 && style < names.length ? names[style] : names[0];
    }

    private void onRowClick(View cell, Row row) {
        Context c = getParentActivity();
        if (c == null) return;
        switch (row.id) {
            case NO_BLUR: case NO_TYPING: case NO_READ: case SAVE_DELETED: case CONFIRM_CALLS:
            case LOCAL_PREMIUM: case HIDE_PREMIUM: case HIDE_STARS: {
                String key = row.id == NO_BLUR ? ModConfig.KEY_NO_BLUR :
                        row.id == NO_TYPING ? ModConfig.KEY_NO_TYPING :
                        row.id == NO_READ ? ModConfig.KEY_NO_READ :
                        row.id == SAVE_DELETED ? ModConfig.KEY_SAVE_DELETED :
                        row.id == CONFIRM_CALLS ? ModConfig.KEY_CONFIRM_CALLS :
                        row.id == LOCAL_PREMIUM ? ModConfig.KEY_LOCAL_PREMIUM :
                        row.id == HIDE_PREMIUM ? ModConfig.KEY_HIDE_PREMIUM : ModConfig.KEY_HIDE_STARS;
                boolean enabled = ModConfig.toggle(key);
                if (cell instanceof TextCheckCell) ((TextCheckCell) cell).setChecked(enabled);
                if (row.id == NO_BLUR || row.id == LOCAL_PREMIUM) showRestartDialog();
                break;
            }
            case 40: {
                boolean enabled = !FerrumConfig.isAntivirusEnabled(c);
                FerrumConfig.setAntivirusEnabled(c, enabled);
                if (cell instanceof TextCheckCell) ((TextCheckCell) cell).setChecked(enabled);
                refresh();
                break;
            }
            case ACCENT: showAccentDialog(); break;
            case COLOR_PACK: showColorPackDialog(); break;
            case UI_STYLE: showUiStyleDialog(); break;
            case BANNER_PICK: openDocument("image/*", REQUEST_BANNER, false); break;
            case BANNER_CONFIGURE: presentFragment(new FerrumBannerEditor()); break;
            case BANNER_REMOVE: FerrumConfig.clearProfileBanner(c); refresh(); break;
            case AVATAR_RING: {
                boolean enabled = !FerrumConfig.isAvatarRingEnabled(c);
                FerrumConfig.setAvatarRingEnabled(c, enabled);
                if (cell instanceof TextCheckCell) ((TextCheckCell) cell).setChecked(enabled);
                break;
            }
            case AVATAR_WIDTH: showAvatarWidthDialog(); break;
            case AVATAR_COLOR: showAvatarColorDialog(); break;
            case AVATAR_STYLE: showAvatarStyleDialog(); break;
            case AV_ENGINE: showEngineDialog(); break;
            case AI_PROVIDER: showProviderDialog(); break;
            case AI_KEY: showSecretDialog(true); break;
            case VT_KEY: showSecretDialog(false); break;
            case SCAN: openDocument("*/*", REQUEST_SCAN, false); break;
            case EXPORT: openDocument("application/json", REQUEST_EXPORT, true); break;
            case IMPORT: openDocument("application/json", REQUEST_IMPORT, false); break;
        }
    }

    private void refresh() { if (getParentActivity() != null) { rebuildRows(getParentActivity()); if (adapter != null) adapter.notifyDataSetChanged(); } }
    private void showRestartDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        b.setTitle(LocaleController.getString(R.string.FerrumRestartTitle));
        b.setMessage(LocaleController.getString(R.string.FerrumRestartText));
        b.setPositiveButton(LocaleController.getString(R.string.FerrumRestartNow), (d, w) -> ModUtils.restartApp());
        b.setNegativeButton(LocaleController.getString(R.string.FerrumLater), null);
        showDialog(b.create());
    }
    private void showUiStyleDialog() {
        String[] names = {
                LocaleController.getString(R.string.FerrumUiPresetDefault),
                LocaleController.getString(R.string.FerrumUiPresetFullWidth),
                LocaleController.getString(R.string.FerrumUiPresetFloating)
        };
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        b.setTitle(LocaleController.getString(R.string.FerrumUiStyle));
        b.setItems(names, (d, which) -> {
            FerrumConfig.setUiPreset(getParentActivity(), which);
            refresh();
            showRestartDialog();
        });
        b.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(b.create());
    }
    private void showAccentDialog() {
        final int[] colors = {0xFF6E9BFF,0xFF56C6A9,0xFFB58AFF,0xFFFF9B70,0xFFE879A8,0xFFE5B85C};
        String[] names = {LocaleController.getString(R.string.FerrumColorBlue),LocaleController.getString(R.string.FerrumColorMint),LocaleController.getString(R.string.FerrumColorLavender),LocaleController.getString(R.string.FerrumColorPeach),LocaleController.getString(R.string.FerrumColorPink),LocaleController.getString(R.string.FerrumColorGold)};
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        b.setTitle(LocaleController.getString(R.string.FerrumAccentColor));
        b.setItems(names, (d, which) -> { FerrumConfig.setAccentColor(getParentActivity(), colors[which]); FerrumConfig.applyAccentTheme(getParentActivity()); refresh(); });
        showDialog(b.create());
    }
    private void showColorPackDialog() {
        CharSequence[] names = new CharSequence[ModTheme.PACK_NAMES.length];
        for (int i=0;i<names.length;i++) names[i]=packName(i);
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        b.setTitle(LocaleController.getString(R.string.FerrumColorPalette));
        b.setItems(names, (d, which) -> { ModConfig.setColorPack(which); ModTheme.applyLive(); refresh(); });
        b.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(b.create());
    }
    private void showAvatarWidthDialog() {
        EditText input=new EditText(getParentActivity()); input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL); input.setText(String.valueOf(FerrumConfig.getAvatarRingWidth(getParentActivity())));
        AlertDialog.Builder b=new AlertDialog.Builder(getParentActivity()); b.setTitle(LocaleController.getString(R.string.FerrumRingWidth)); b.setView(input);
        b.setPositiveButton(LocaleController.getString(R.string.OK),(d,w)->{try{FerrumConfig.setAvatarRingWidth(getParentActivity(),Math.max(1f,Math.min(12f,Float.parseFloat(input.getText().toString()))));refresh();}catch(Exception ignored){}});
        b.setNegativeButton(LocaleController.getString(R.string.Cancel),null); showDialog(b.create());
    }
    private void showAvatarColorDialog() {
        int[] colors={0xFF6E9BFF,0xFF56C6A9,0xFFB58AFF,0xFFFF9B70,Color.WHITE};
        String[] names={LocaleController.getString(R.string.FerrumColorBlue),LocaleController.getString(R.string.FerrumColorMint),LocaleController.getString(R.string.FerrumColorLavender),LocaleController.getString(R.string.FerrumColorPeach),LocaleController.getString(R.string.FerrumColorWhite)};
        AlertDialog.Builder b=new AlertDialog.Builder(getParentActivity()); b.setTitle(LocaleController.getString(R.string.FerrumRingColor));
        b.setItems(names,(d,w)->{FerrumConfig.setAvatarRingColor(getParentActivity(),colors[w]);refresh();}); showDialog(b.create());
    }
    private void showAvatarStyleDialog() {
        String[] styles={LocaleController.getString(R.string.FerrumRingSolid),LocaleController.getString(R.string.FerrumRingGradient),LocaleController.getString(R.string.FerrumRingDashed)}; AlertDialog.Builder b=new AlertDialog.Builder(getParentActivity()); b.setTitle(LocaleController.getString(R.string.FerrumRingStyle));
        b.setItems(styles,(d,w)->{FerrumConfig.setAvatarRingStyle(getParentActivity(),w);refresh();}); showDialog(b.create());
    }
    private void showEngineDialog() {
        String[] engines={LocaleController.getString(R.string.FerrumEngineAi),"VirusTotal"}; AlertDialog.Builder b=new AlertDialog.Builder(getParentActivity()); b.setTitle(LocaleController.getString(R.string.FerrumAvEngine));
        b.setItems(engines,(d,w)->{FerrumConfig.setAntivirusEngine(getParentActivity(),w==1?FerrumConfig.ANTIVIRUS_ENGINE_VIRUSTOTAL:FerrumConfig.ANTIVIRUS_ENGINE_AI);refresh();}); showDialog(b.create());
    }
    private void showProviderDialog() {
        String[] providers={"Gemini","Claude","ChatGPT"}; AlertDialog.Builder b=new AlertDialog.Builder(getParentActivity()); b.setTitle(LocaleController.getString(R.string.FerrumAiProvider));
        b.setItems(providers,(d,w)->{FerrumConfig.setAiProvider(getParentActivity(),providers[w]);refresh();}); showDialog(b.create());
    }
    private void showSecretDialog(boolean ai) {
        Context c=getParentActivity(); EditText input=new EditText(c); input.setSingleLine(true); input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint(LocaleController.getString(R.string.FerrumKeyHint)); input.setText(ai?FerrumConfig.getAiApiKey(c):FerrumConfig.getVirusTotalApiKey(c));
        AlertDialog.Builder b=new AlertDialog.Builder(c); b.setTitle(ai?LocaleController.getString(R.string.FerrumAiKey):LocaleController.getString(R.string.FerrumVtKey)); b.setView(input);
        b.setPositiveButton(LocaleController.getString(R.string.OK),(d,w)->{try{if(ai)FerrumConfig.setAiApiKey(c,input.getText().toString());else FerrumConfig.setVirusTotalApiKey(c,input.getText().toString());Toast.makeText(c,LocaleController.getString(R.string.FerrumKeySaved),Toast.LENGTH_SHORT).show();}catch(Exception e){Toast.makeText(c,LocaleController.getString(R.string.FerrumKeySaveFailed),Toast.LENGTH_LONG).show();}refresh();});
        b.setNegativeButton(LocaleController.getString(R.string.Cancel),null); showDialog(b.create());
    }
    private void openDocument(String mime,int request,boolean create) {
        Intent i=new Intent(create?Intent.ACTION_CREATE_DOCUMENT:Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType(mime);
        if(create){i.putExtra(Intent.EXTRA_TITLE,"ferrum-settings.json");} else {i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);}
        startActivityForResult(i,request);
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {
        private final Context context;
        ListAdapter(Context c){context=c;}
        @Override public int getItemCount(){return rows.size();}
        @Override public int getItemViewType(int p){return rows.get(p).type;}
        @Override public boolean isEnabled(RecyclerView.ViewHolder h){int p=h.getAdapterPosition();return p>=0&&p<rows.size()&&(rows.get(p).type==CHECK||rows.get(p).type==VALUE);}
        @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,int type){
            View v;
            if (type==CHECK) v=new TextCheckCell(context);
            else if (type==VALUE) v=new TextSettingsCell(context);
            else if (type==PREVIEW_BLUR) v=new ModBlurPreviewView(context);
            else if (type==PREVIEW_COLOR) v=new ModColorPreviewView(context);
            else if (type==PREVIEW_PROFILE) v=new ModProfilePreviewView(context);
            else v=new TextInfoPrivacyCell(context);
            v.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT,RecyclerView.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(v);
        }
        @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h,int p){
            Row r=rows.get(p); boolean divider=p+1<rows.size()&&(rows.get(p+1).type==CHECK||rows.get(p+1).type==VALUE);
            if(r.type==CHECK)((TextCheckCell)h.itemView).setTextAndCheck(r.title,r.checked,divider);
            else if(r.type==VALUE)((TextSettingsCell)h.itemView).setTextAndValue(r.title,r.value==null?"":r.value,divider);
            else if(r.type==PREVIEW_BLUR){h.itemView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));((ModBlurPreviewView)h.itemView).setBlurDisabled(ModConfig.isNoBlur());}
            else if(r.type==PREVIEW_COLOR){h.itemView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));((ModColorPreviewView)h.itemView).setPack(ModConfig.getColorPack());}
            else if(r.type==PREVIEW_PROFILE){h.itemView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));((ModProfilePreviewView)h.itemView).refresh();}
            else {
                TextInfoPrivacyCell shadowCell=(TextInfoPrivacyCell)h.itemView;
                shadowCell.setText(r.title);
                shadowCell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
            }
        }
    }

    @Override
    public void onActivityResultFragment(int requestCode,int resultCode,Intent data) {
        if(resultCode!=Activity.RESULT_OK||data==null||data.getData()==null)return;
        Uri uri=data.getData(); Context c=getParentActivity();
        if (c == null) return;
        try {
            if(requestCode==REQUEST_EXPORT){
                String json=FerrumConfig.exportPortableSettings(c);
                try(OutputStream out=c.getContentResolver().openOutputStream(uri)){if(out==null)throw new IllegalStateException("Не удалось открыть файл");out.write(json.getBytes(StandardCharsets.UTF_8));}
                Toast.makeText(c,LocaleController.getString(R.string.FerrumExportedToast),Toast.LENGTH_LONG).show();
            } else if(requestCode==REQUEST_IMPORT){
                StringBuilder json=new StringBuilder();
                try(InputStream in=c.getContentResolver().openInputStream(uri)){if(in==null)throw new IllegalStateException("Не удалось открыть файл");byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1){if(json.length()+n>128*1024)throw new IllegalArgumentException("Файл слишком большой");json.append(new String(buf,0,n,StandardCharsets.UTF_8));}}
                FerrumConfig.importPortableSettings(c,json.toString()); FerrumConfig.applyAccentTheme(c); ModTheme.applyLive();
                Toast.makeText(c,LocaleController.getString(R.string.FerrumImportedToast),Toast.LENGTH_LONG).show(); refresh();
            } else if(requestCode==REQUEST_BANNER){
                c.getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION); FerrumConfig.setProfileBannerUri(c,uri.toString()); refresh();
            } else if(requestCode==REQUEST_SCAN){
                FerrumAntivirus.scan(c,uri,result->{android.app.Activity activity=getParentActivity();if(activity==null)return;StringBuilder msg=new StringBuilder();msg.append(String.format(LocaleController.getString(R.string.FerrumScanFileLabel), result.fileName)).append("\n").append(String.format(LocaleController.getString(R.string.FerrumScanSizeLabel), result.size)).append("\n").append(String.format(LocaleController.getString(R.string.FerrumScanShaLabel), result.sha256)).append("\n").append(String.format(LocaleController.getString(R.string.FerrumScanRiskLabel), result.riskLabel()));for(String finding:result.findings)msg.append("\n• ").append(finding);new AlertDialog.Builder(activity).setTitle(LocaleController.getString(R.string.FerrumScanResultTitle)).setMessage(msg.toString()).setPositiveButton(LocaleController.getString(R.string.FerrumClose),null).show();});
            }
        } catch(Exception e){Toast.makeText(c, String.format(LocaleController.getString(R.string.FerrumErrorToast), e.getMessage()),Toast.LENGTH_LONG).show();}
    }

}
