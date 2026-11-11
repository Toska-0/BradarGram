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
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
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

    private static final int ACCENT = 1, BANNER_PICK = 2, BANNER_REMOVE = 3,
            BANNER_SCALE = 4, BANNER_X = 5, BANNER_Y = 6,
            AVATAR_RING = 7, AVATAR_WIDTH = 8, AVATAR_COLOR = 9, AVATAR_STYLE = 10,
            AV_ENGINE = 11, AI_PROVIDER = 12, AI_KEY = 13, VT_KEY = 14,
            EXPORT = 15, IMPORT = 16, SCAN = 17, COLOR_PACK = 18;
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

        FrameLayout frame = new FrameLayout(context);
        fragmentView = frame;
        frame.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        rebuildRows(context);
        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context));
        listView.setVerticalScrollBarEnabled(false);
        listView.setAdapter(adapter = new ListAdapter(context));
        listView.setOnItemClickListener((view, position) -> {
            if (position >= 0 && position < rows.size()) onRowClick(rows.get(position));
        });
        frame.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        return fragmentView;
    }

    private void header(String title) { rows.add(new Row(HEADER, 0, title, null, false)); }
    private void check(int id, String title, boolean checked) { rows.add(new Row(CHECK, id, title, null, checked)); }
    private void value(int id, String title, String value) { rows.add(new Row(VALUE, id, title, value, false)); }
    private void info(String text) { rows.add(new Row(INFO, 0, text, null, false)); }

    private void rebuildRows(Context c) {
        rows.clear();
        header("ОФОРМЛЕНИЕ");
        value(ACCENT, "Акцентный цвет", "Изменить");
        check(NO_BLUR, "Отключить размытие и Liquid Glass", ModConfig.isNoBlur());
        value(COLOR_PACK, "Цветовая палитра", packName(ModConfig.getColorPack()));
        info("Изменения размытия могут потребовать перезапуска приложения.");

        header("БАННЕР ПРОФИЛЯ");
        value(BANNER_PICK, "Выбрать изображение", "Галерея");
        value(BANNER_REMOVE, "Удалить баннер", null);
        value(BANNER_SCALE, "Масштаб баннера", percent(FerrumConfig.getBannerScale(c)));
        value(BANNER_X, "Положение по горизонтали", percent(FerrumConfig.getBannerOffsetX(c)));
        value(BANNER_Y, "Положение по вертикали", percent(FerrumConfig.getBannerOffsetY(c)));

        header("АВАТАР");
        check(AVATAR_RING, "Включить обводку аватара", FerrumConfig.isAvatarRingEnabled(c));
        value(AVATAR_WIDTH, "Толщина обводки", String.valueOf(FerrumConfig.getAvatarRingWidth(c)));
        value(AVATAR_COLOR, "Цвет обводки", "Выбрать");
        value(AVATAR_STYLE, "Стиль обводки", ringStyle(FerrumConfig.getAvatarRingStyle(c)));

        header("БЕЗОПАСНОСТЬ");
        check(40, "Показывать пункт проверки файлов", FerrumConfig.isAntivirusEnabled(c));
        if (FerrumConfig.isAntivirusEnabled(c)) {
        value(AV_ENGINE, "Движок анализа", FerrumConfig.getAntivirusEngine(c) == FerrumConfig.ANTIVIRUS_ENGINE_VIRUSTOTAL ? "VirusTotal" : "AI-анализ");
        value(AI_PROVIDER, "AI-провайдер", FerrumConfig.getAiProvider(c));
        value(AI_KEY, "API-ключ AI", masked(FerrumConfig.getAiApiKey(c)));
        value(VT_KEY, "API-ключ VirusTotal", masked(FerrumConfig.getVirusTotalApiKey(c)));
        value(SCAN, "Проверить файл", "Выбрать файл");
        info("Сетевые запросы к AI и VirusTotal пока не подключены. Доступен локальный эвристический анализ.");
        }

        header("ПРИВАТНОСТЬ И СООБЩЕНИЯ");
        check(NO_TYPING, "Не отправлять статус «печатает…»", ModConfig.get(ModConfig.KEY_NO_TYPING));
        check(NO_READ, "Читать без отметки о прочтении", ModConfig.get(ModConfig.KEY_NO_READ));
        check(SAVE_DELETED, "Сохранять удалённые сообщения", ModConfig.get(ModConfig.KEY_SAVE_DELETED));
        check(CONFIRM_CALLS, "Подтверждать исходящие звонки", ModConfig.get(ModConfig.KEY_CONFIRM_CALLS));

        header("TELEGRAM");
        check(LOCAL_PREMIUM, "Локальный Telegram Premium", ModConfig.get(ModConfig.KEY_LOCAL_PREMIUM));
        check(HIDE_PREMIUM, "Скрыть Telegram Premium", ModConfig.get(ModConfig.KEY_HIDE_PREMIUM));
        check(HIDE_STARS, "Скрыть Telegram Stars", ModConfig.get(ModConfig.KEY_HIDE_STARS));
        info("Локальный Premium меняет только отображение на устройстве. Серверные функции требуют настоящую подписку.");

        header("СИНХРОНИЗАЦИЯ");
        value(EXPORT, "Экспортировать настройки", "JSON");
        value(IMPORT, "Импортировать настройки", "JSON");
        info("Облачная синхронизация пока не подключена. Баннер профиля не переносится. Экспорт не содержит токены и переписки.");

        header("О FERRUM");
        value(0, "Версия Ferrum", ModConfig.MOD_VERSION);
    }

    private String percent(float value) { return Math.round(value * 100) + "%"; }
    private String masked(String key) { return key == null || key.isEmpty() ? "Не задан" : "••••••••"; }
    private String packName(int pack) {
        if (pack < 0 || pack >= ModTheme.PACK_NAMES.length) pack = 0;
        return LocaleController.getString(ModTheme.PACK_NAMES[pack]);
    }
    private String ringStyle(int style) {
        String[] names = {"Сплошная", "Градиент", "Пунктир"};
        return style >= 0 && style < names.length ? names[style] : names[0];
    }

    private void onRowClick(Row row) {
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
                if (row.id == NO_BLUR || row.id == LOCAL_PREMIUM) showRestartDialog();
                refresh();
                break;
            }
            case 40:
                FerrumConfig.setAntivirusEnabled(c, !FerrumConfig.isAntivirusEnabled(c)); refresh(); break;
            case ACCENT: showAccentDialog(); break;
            case COLOR_PACK: showColorPackDialog(); break;
            case BANNER_PICK: openDocument("image/*", REQUEST_BANNER, false); break;
            case BANNER_REMOVE: FerrumConfig.clearProfileBanner(c); refresh(); break;
            case BANNER_SCALE: case BANNER_X: case BANNER_Y: showNumberDialog(row.id); break;
            case AVATAR_RING: FerrumConfig.setAvatarRingEnabled(c, !FerrumConfig.isAvatarRingEnabled(c)); refresh(); break;
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
        b.setTitle("Нужен перезапуск");
        b.setMessage("Перезапустить Ferrum, чтобы применить изменение?");
        b.setPositiveButton("Перезапустить", (d, w) -> ModUtils.restartApp());
        b.setNegativeButton("Позже", null);
        showDialog(b.create());
    }
    private void showAccentDialog() {
        final int[] colors = {0xFF6E9BFF,0xFF56C6A9,0xFFB58AFF,0xFFFF9B70,0xFFE879A8,0xFFE5B85C};
        String[] names = {"Синий","Мятный","Сиреневый","Персиковый","Розовый","Золотой"};
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        b.setTitle("Акцентный цвет");
        b.setItems(names, (d, which) -> { FerrumConfig.setAccentColor(getParentActivity(), colors[which]); FerrumConfig.applyAccentTheme(getParentActivity()); refresh(); });
        showDialog(b.create());
    }
    private void showColorPackDialog() {
        CharSequence[] names = new CharSequence[ModTheme.PACK_NAMES.length];
        for (int i=0;i<names.length;i++) names[i]=packName(i);
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        b.setTitle("Цветовая палитра");
        b.setItems(names, (d, which) -> { ModConfig.setColorPack(which); ModTheme.applyLive(); refresh(); });
        b.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(b.create());
    }
    private void showNumberDialog(int id) {
        Context c=getParentActivity();
        EditText input=new EditText(c); input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
        int current=id==BANNER_SCALE ? Math.round(FerrumConfig.getBannerScale(c)*100) : Math.round((id==BANNER_X?FerrumConfig.getBannerOffsetX(c):FerrumConfig.getBannerOffsetY(c))*100);
        input.setText(String.valueOf(current));
        AlertDialog.Builder b=new AlertDialog.Builder(c); b.setTitle(id==BANNER_SCALE?"Масштаб баннера (%)":"Смещение баннера (%)"); b.setView(input);
        b.setPositiveButton(LocaleController.getString(R.string.OK), (d,w)->{ try { int v=Integer.parseInt(input.getText().toString()); if(id==BANNER_SCALE) FerrumConfig.setBannerScale(c,Math.max(50,Math.min(300,v))/100f); else if(id==BANNER_X) FerrumConfig.setBannerOffsetX(c,Math.max(-100,Math.min(100,v))/100f); else FerrumConfig.setBannerOffsetY(c,Math.max(-100,Math.min(100,v))/100f); refresh(); } catch(Exception ignored){} });
        b.setNegativeButton(LocaleController.getString(R.string.Cancel),null); showDialog(b.create());
    }
    private void showAvatarWidthDialog() {
        EditText input=new EditText(getParentActivity()); input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL); input.setText(String.valueOf(FerrumConfig.getAvatarRingWidth(getParentActivity())));
        AlertDialog.Builder b=new AlertDialog.Builder(getParentActivity()); b.setTitle("Толщина обводки"); b.setView(input);
        b.setPositiveButton(LocaleController.getString(R.string.OK),(d,w)->{try{FerrumConfig.setAvatarRingWidth(getParentActivity(),Math.max(1f,Math.min(12f,Float.parseFloat(input.getText().toString()))));refresh();}catch(Exception ignored){}});
        b.setNegativeButton(LocaleController.getString(R.string.Cancel),null); showDialog(b.create());
    }
    private void showAvatarColorDialog() {
        int[] colors={0xFF6E9BFF,0xFF56C6A9,0xFFB58AFF,0xFFFF9B70,Color.WHITE};
        String[] names={"Синий","Мятный","Сиреневый","Персиковый","Белый"};
        AlertDialog.Builder b=new AlertDialog.Builder(getParentActivity()); b.setTitle("Цвет обводки");
        b.setItems(names,(d,w)->{FerrumConfig.setAvatarRingColor(getParentActivity(),colors[w]);refresh();}); showDialog(b.create());
    }
    private void showAvatarStyleDialog() {
        String[] styles={"Сплошная","Градиент","Пунктир"}; AlertDialog.Builder b=new AlertDialog.Builder(getParentActivity()); b.setTitle("Стиль обводки");
        b.setItems(styles,(d,w)->{FerrumConfig.setAvatarRingStyle(getParentActivity(),w);refresh();}); showDialog(b.create());
    }
    private void showEngineDialog() {
        String[] engines={"AI-анализ","VirusTotal"}; AlertDialog.Builder b=new AlertDialog.Builder(getParentActivity()); b.setTitle("Движок анализа");
        b.setItems(engines,(d,w)->{FerrumConfig.setAntivirusEngine(getParentActivity(),w==1?FerrumConfig.ANTIVIRUS_ENGINE_VIRUSTOTAL:FerrumConfig.ANTIVIRUS_ENGINE_AI);refresh();}); showDialog(b.create());
    }
    private void showProviderDialog() {
        String[] providers={"Gemini","Claude","ChatGPT"}; AlertDialog.Builder b=new AlertDialog.Builder(getParentActivity()); b.setTitle("AI-провайдер");
        b.setItems(providers,(d,w)->{FerrumConfig.setAiProvider(getParentActivity(),providers[w]);refresh();}); showDialog(b.create());
    }
    private void showSecretDialog(boolean ai) {
        Context c=getParentActivity(); EditText input=new EditText(c); input.setSingleLine(true); input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint("API-ключ"); input.setText(ai?FerrumConfig.getAiApiKey(c):FerrumConfig.getVirusTotalApiKey(c));
        AlertDialog.Builder b=new AlertDialog.Builder(c); b.setTitle(ai?"AI API-ключ":"API-ключ VirusTotal"); b.setView(input);
        b.setPositiveButton(LocaleController.getString(R.string.OK),(d,w)->{try{if(ai)FerrumConfig.setAiApiKey(c,input.getText().toString());else FerrumConfig.setVirusTotalApiKey(c,input.getText().toString());Toast.makeText(c,"Ключ сохранён",Toast.LENGTH_SHORT).show();}catch(Exception e){Toast.makeText(c,"Не удалось сохранить ключ",Toast.LENGTH_LONG).show();}refresh();});
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
            View v=type==HEADER?new HeaderCell(context):type==CHECK?new TextCheckCell(context):type==VALUE?new TextSettingsCell(context):new TextInfoPrivacyCell(context);
            v.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT,RecyclerView.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(v);
        }
        @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h,int p){
            Row r=rows.get(p); boolean divider=p+1<rows.size()&&(rows.get(p+1).type==CHECK||rows.get(p+1).type==VALUE);
            if(r.type==HEADER)((HeaderCell)h.itemView).setText(r.title);
            else if(r.type==CHECK)((TextCheckCell)h.itemView).setTextAndCheck(r.title,r.checked,divider);
            else if(r.type==VALUE)((TextSettingsCell)h.itemView).setTextAndValue(r.title,r.value==null?"":r.value,divider);
            else ((TextInfoPrivacyCell)h.itemView).setText(r.title);
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
                Toast.makeText(c,"Настройки Ferrum экспортированы",Toast.LENGTH_LONG).show();
            } else if(requestCode==REQUEST_IMPORT){
                StringBuilder json=new StringBuilder();
                try(InputStream in=c.getContentResolver().openInputStream(uri)){if(in==null)throw new IllegalStateException("Не удалось открыть файл");byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1){if(json.length()+n>128*1024)throw new IllegalArgumentException("Файл слишком большой");json.append(new String(buf,0,n,StandardCharsets.UTF_8));}}
                FerrumConfig.importPortableSettings(c,json.toString()); FerrumConfig.applyAccentTheme(c); ModTheme.applyLive();
                Toast.makeText(c,"Настройки импортированы",Toast.LENGTH_LONG).show(); refresh();
            } else if(requestCode==REQUEST_BANNER){
                c.getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION); FerrumConfig.setProfileBannerUri(c,uri.toString()); refresh();
            } else if(requestCode==REQUEST_SCAN){
                FerrumAntivirus.scan(c,uri,result->{android.app.Activity activity=getParentActivity();if(activity==null)return;StringBuilder msg=new StringBuilder();msg.append("Файл: ").append(result.fileName).append("\nРазмер: ").append(result.size).append(" байт\nSHA-256: ").append(result.sha256).append("\nУровень риска: ").append(result.riskLabel());for(String finding:result.findings)msg.append("\n• ").append(finding);new AlertDialog.Builder(activity).setTitle("Результат проверки").setMessage(msg.toString()).setPositiveButton("Закрыть",null).show();});
            }
        } catch(Exception e){Toast.makeText(c,"Ошибка: "+e.getMessage(),Toast.LENGTH_LONG).show();}
    }

}
