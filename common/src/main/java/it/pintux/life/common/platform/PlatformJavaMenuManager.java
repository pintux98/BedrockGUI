package it.pintux.life.common.platform;

import it.pintux.life.common.form.FormMenuUtil;
import it.pintux.life.common.form.obj.FormMenu;
import it.pintux.life.common.utils.FormPlayer;

import java.util.Map;

public interface PlatformJavaMenuManager {
    void openJavaMenu(FormPlayer player, FormMenu menu, Map<String, String> placeholders, FormMenuUtil util);
}

