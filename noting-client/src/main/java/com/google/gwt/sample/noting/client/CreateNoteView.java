package com.google.gwt.sample.noting.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class CreateNoteView extends Composite {

    interface CreateNoteViewUiBinder extends UiBinder<Widget, CreateNoteView> {}
    private static CreateNoteViewUiBinder uiBinder = GWT.create(CreateNoteViewUiBinder.class);

    @UiField TextBox titoloBox;
    @UiField TextArea contenutoBox;
    @UiField Button saveButton;
    @UiField Button backButton;

    private CreateNoteViewListener listener;

    public CreateNoteView() {
        initWidget(uiBinder.createAndBindUi(this));
    }

    public void setCreateNoteViewListener(CreateNoteViewListener listener) {
        this.listener = listener;
    }

    @UiHandler("saveButton")
    void onSaveClick(ClickEvent e) {
        if (listener != null) {
            listener.onSave(titoloBox.getText(), contenutoBox.getText());
        }
    }

    @UiHandler("backButton")
    void onBackClick(ClickEvent e) {
        if (listener != null) {
            listener.onBack();
        }
    }
   
}