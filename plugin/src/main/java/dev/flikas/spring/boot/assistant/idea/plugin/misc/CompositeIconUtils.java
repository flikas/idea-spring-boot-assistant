package dev.flikas.spring.boot.assistant.idea.plugin.misc;

import com.intellij.ui.IconManager;
import com.intellij.ui.JBColor;
import com.intellij.ui.OffsetIcon;
import com.intellij.ui.RetrievableIcon;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.ColorIcon;
import com.intellij.util.ui.JBCachingScalableIcon;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

import static com.intellij.ui.scale.ScaleType.OBJ_SCALE;
import static java.lang.Math.ceil;
import static java.lang.Math.floor;

public interface CompositeIconUtils {
  static Icon createWithModifier(Icon baseIcon, Icon modifierIcon) {
    IconManager im = IconManager.getInstance();
    int halfWidth = OffsetIcon.REGULAR_OFFSET / 2;
    Icon modifierBackground = new ColorIcon(modifierIcon.getIconWidth(), modifierIcon.getIconHeight(),
        modifierIcon.getIconWidth(), modifierIcon.getIconHeight(),
        JBColor.background(), false);
    Icon modifierWithBackground = im.createLayered(modifierBackground, modifierIcon);
    var maskLayer = new ModifierIcon(IconUtil.resizeSquared(modifierWithBackground, halfWidth));
    return im.createLayered(baseIcon, maskLayer);
  }


  class ModifierIcon extends JBCachingScalableIcon<ModifierIcon> implements RetrievableIcon {
    private final double factor = 0.8;
    private final Icon myIcon;
    private int myWidth;
    private int myHeight;
    private Icon myScaledIcon;
    private int myScaledXOffset;
    private int myScaledYOffset;

    {
      getScaleContext().addUpdateListener(this::updateSize);
      setAutoUpdateScaleContext(false);
    }


    public ModifierIcon(@NotNull Icon icon) {
      myIcon = icon;
      updateSize();
    }

    private ModifierIcon(@NotNull ModifierIcon icon) {
      super(icon);
      myWidth = icon.myWidth;
      myHeight = icon.myHeight;
      myIcon = icon.myIcon;
      myScaledIcon = null;
      myScaledXOffset = icon.myScaledXOffset;
      myScaledYOffset = icon.myScaledYOffset;
    }

    @Override
    public @NotNull ModifierIcon copy() {
      return new ModifierIcon(this);
    }

    public @NotNull Icon getIcon() {
      return myIcon;
    }

    public int hashCode() {
      return myIcon.hashCode();
    }

    public boolean equals(Object obj) {
      if (obj == this) return true;
      if (obj instanceof ModifierIcon icon) {
        return Objects.equals(icon.myIcon, myIcon);
      }
      return false;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
      getScaleContext().update();
      if (myScaledIcon == null) {
        float scale = getScale();
        myScaledIcon = scale == 1f ? myIcon : IconUtil.scale(myIcon, null, scale);
      }
      myScaledIcon.paintIcon(c, g, myScaledXOffset + x, myScaledYOffset + y);
    }

    @Override
    public int getIconWidth() {
      getScaleContext().update();
      return (int) ceil(scaleVal(myWidth, OBJ_SCALE) * (1 + factor));
    }

    @Override
    public int getIconHeight() {
      getScaleContext().update();
      return (int) ceil(scaleVal(myHeight, OBJ_SCALE) * (1 + factor));
    }

//    FIXME: incompatible upgrade on IconReplacer from JBR 241+
//    @Override
//    public @NotNull Icon replaceBy(@NotNull IconReplacer replacer) {
//      return new ModifierIcon(replacer.replaceIcon(myIcon));
//    }

    private void updateSize() {
      myWidth = myIcon.getIconWidth();
      myHeight = myIcon.getIconHeight();
      myScaledXOffset = (int) floor(scaleVal(myWidth, OBJ_SCALE) * factor);
      myScaledYOffset = (int) floor(scaleVal(myHeight, OBJ_SCALE) * factor);
    }

    @Override
    public String toString() {
      return "ModifierIcon: icon=" + myIcon;
    }

    @Override
    public @NotNull Icon retrieveIcon() {
      return getIcon();
    }
  }
}
