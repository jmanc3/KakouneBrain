package com.jmanc3.kakounebrain.input.implementation.other;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.ui.JBColor;
import com.jmanc3.kakounebrain.PluginStartup;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class MenuRenderer extends JComponent {

    private JComponent contentComponent;
    private Editor editor;
    private static final Color color = new JBColor(new Color(255, 188, 33), new Color(255, 188, 33));

    public ArrayList<String> text = new ArrayList<>();

    public ArrayList<Editor> editors = new ArrayList<>();

    public void showIt(Editor editor) {
        this.editor = editor;
        editors.add(editor);
        contentComponent = this.editor.getContentComponent();
        contentComponent.remove(this);
        PluginStartup.removeAllShortcuts();
        PluginStartup.addMenuBind();
        contentComponent.add(this);

        Font font = editor.getColorsScheme().getFont(EditorFontType.PLAIN);
        FontMetrics fontMetrics = editor.getComponent().getFontMetrics(font);
        lineHeight = fontMetrics.getHeight();
        lineAscent = fontMetrics.getAscent();
        textH = fontMetrics.getHeight() * text.size();
        textW = 0;
        for (String s : text) {
            int width = fontMetrics.stringWidth(s);
            if (textW < width)
                textW = width;
        }

        setBounds(0, 0, contentComponent.getWidth(), contentComponent.getHeight());
        contentComponent.getParent().repaint();
    }

    public void hideIt(Editor editor) {
        this.editor = editor;
        editors.remove(editor);
        contentComponent = this.editor.getContentComponent();
        text.clear();
        setBounds(0, 0, 0, 0);
        contentComponent.getParent().repaint();
        contentComponent.remove(this);
    }

    private int textW = 0;

    private int textH = 0;

    private int lineHeight = 0;

    private int lineAscent = 0;

    private int offsetFromWall = 20; // this should be based on dpi as well

    private int beforeInternalBorder = 6;

    private int internalBorderThickness = 2; // TODO: make this based on dpi, 1 on 1080, 2 on 1444, 3 otherwise

    private int borderToText = 8;

    @Override
    protected void paintChildren(Graphics g) {
        if (text.isEmpty()) {
            return;
        }
        super.paintChildren(g);

        int boxW = textW + beforeInternalBorder * 2 + internalBorderThickness * 2 + borderToText * 2;
        int boxH = textH + beforeInternalBorder * 2 + internalBorderThickness * 2 + borderToText * 2;
        int boxX = getParent().getParent().getWidth() - offsetFromWall - boxW - getParent().getX();
        int boxY = getParent().getParent().getHeight() - offsetFromWall - boxH - getParent().getY();
        g.setColor(Color.BLACK);
        g.fillRect(boxX + 2, boxY + 2, boxW, boxH);
        g.setColor(color);
        g.fillRect(boxX, boxY, boxW, boxH);

        boxX += beforeInternalBorder;
        boxY += beforeInternalBorder;
        boxW -= beforeInternalBorder * 2;
        boxH -= beforeInternalBorder * 2;
        g.setColor(Color.BLACK);
        g.fillRect(boxX, boxY, boxW, boxH);

        boxX += internalBorderThickness;
        boxY += internalBorderThickness;
        boxW -= internalBorderThickness * 2;
        boxH -= internalBorderThickness * 2;
        g.setColor(color);
        g.fillRect(boxX, boxY, boxW, boxH);

        boxX += borderToText;
        boxY += borderToText + lineAscent;
        ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.BLACK);
        Font font = editor.getColorsScheme().getFont(EditorFontType.PLAIN);
        g.setFont(font);
        for (String line : text) {
            g.drawString(line, boxX, boxY);
            boxY += lineHeight;
        }
    }

    public void hideAll() {
        for (Editor editor : editors) {
            hideIt(editor);
        }
    }
}
