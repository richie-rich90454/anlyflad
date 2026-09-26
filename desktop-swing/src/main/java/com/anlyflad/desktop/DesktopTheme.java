package com.anlyflad.desktop;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
public final class DesktopTheme {
    public static final Color WINDOW=new Color(0xF3F0E8);
    public static final Color SURFACE=new Color(0xFFFCF6);
    public static final Color CANVAS=new Color(0xD8D4CA);
    public static final Color PAGE=new Color(0xFFFFFF);
    public static final Color TEXT=new Color(0x25231F);
    public static final Color MUTED_TEXT=new Color(0x6F6A61);
    public static final Color BORDER=new Color(0xB8B1A4);
    public static final Color ACCENT=new Color(0xD65A31);
    public static final Color ACCENT_HOVER=new Color(0xB94724);
    public static final Color SUCCESS=new Color(0x397A4B);
    public static final Color WARNING=new Color(0xA96518);
    public static final Color ERROR=new Color(0xB13E3E);
    public static final Color ACCENT_SOFT=new Color(0xF3DED6);
    public static final Color SURFACE_ALT=new Color(0xF7F3EA);
    public static final Font TITLE_FONT=load("/fonts/NotoSans-Bold.ttf", Font.BOLD, 22);
    public static final Font DISPLAY_FONT=load("/fonts/NotoSans-Bold.ttf", Font.BOLD, 18);
    public static final Font LABEL_FONT=load("/fonts/NotoSans-Bold.ttf", Font.BOLD, 12);
    public static final Font BODY_FONT=load("/fonts/NotoSans-Regular.ttf", Font.PLAIN, 13);
    public static final Font CAPTION_FONT=load("/fonts/NotoSans-Regular.ttf", Font.PLAIN, 11);
    private DesktopTheme() {
    }
    public static void installUiFonts() {
        FontUIResource regular=new FontUIResource(load("/fonts/NotoSans-Regular.ttf", Font.PLAIN, 13));
        FontUIResource bold=new FontUIResource(load("/fonts/NotoSans-Bold.ttf", Font.BOLD, 13));
        String[] regularKeys={"Button.font", "CheckBox.font", "ComboBox.font", "Label.font", "List.font", "Menu.font", "MenuBar.font", "MenuItem.font", "Panel.font", "RadioButton.font", "TextArea.font", "TextField.font", "TitledBorder.font", "ToggleButton.font", "ToolTip.font", "OptionPane.font", "OptionPane.messageFont", "FileChooser.font", "FileChooser.listFont", "Slider.font", "TabbedPane.font", "Table.font", "TableHeader.font", "Tree.font", "ProgressBar.font", "Spinner.font", "FormattedTextField.font", "PasswordField.font", "EditorPane.font", "TextPane.font"};
        for (int index=0;index<regularKeys.length;index++) {
            UIManager.put(regularKeys[index], regular);
        }
        String[] boldKeys={"OptionPane.buttonFont", "InternalFrame.titleFont", "MenuBar.font"};
        for (int index=0;index<boldKeys.length;index++) {
            UIManager.put(boldKeys[index], bold);
        }
    }
    private static Font load(String path, int style, int size) {
        try {
            InputStream stream=DesktopTheme.class.getResourceAsStream(path);
            if (stream!=null) {
                try {
                    Font font=Font.createFont(Font.TRUETYPE_FONT, stream);
                    if (!GraphicsEnvironment.isHeadless()) {
                        GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
                    }
                    return font.deriveFont(style, (float)size);
                } finally {
                    stream.close();
                }
            }
        } catch (IOException exception) {
        } catch (FontFormatException exception) {
        }
        return new Font(Font.SANS_SERIF, style, size);
    }
}
