package com.google.gwt.sample.noting.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.sample.noting.shared.Note;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.*;

public class VisualizzaNotaView extends Composite {

    interface VisualizzaNotaViewUiBinder extends UiBinder<Widget, VisualizzaNotaView> {}
    private static VisualizzaNotaViewUiBinder uiBinder = GWT.create(VisualizzaNotaViewUiBinder.class);

    @UiField Label titoloLabel;
    @UiField TextArea contenutoArea;
    @UiField Button backButton;

    private VisualizzaNotaViewListener listener;

    public VisualizzaNotaView(Note nota) {
        initWidget(uiBinder.createAndBindUi(this));
        titoloLabel.setText(nota.getTitle());
        contenutoArea.setText(nota.getContent());
        contenutoArea.setReadOnly(true);
    }

    public void setVisualizzaNotaViewListener(VisualizzaNotaViewListener listener) {
        this.listener = listener;
    }

    @UiHandler("backButton")
    void onBackClick(ClickEvent e) {
        if (listener != null) {
            listener.onBack();
        }
    }
}
