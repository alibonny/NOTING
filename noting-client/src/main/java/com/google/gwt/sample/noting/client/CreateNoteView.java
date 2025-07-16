package com.google.gwt.sample.noting.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.sample.noting.client.CreateNoteView.CreateNoteViewUiBinder;
import com.google.gwt.sample.noting.shared.User;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class CreateNoteView extends Composite {

    interface CreateNoteViewUiBinder extends UiBinder<Widget, CreateNoteView> {}
    private static CreateNoteViewUiBinder uiBinder = GWT.create(CreateNoteViewUiBinder.class);

    @UiField TextBox titleBox;
    @UiField TextArea contentArea;
    @UiField Button saveButton;
    @UiField Button cancelButton;
    @UiField Button backButton;

    public CreateNoteView() {
        initWidget(uiBinder.createAndBindUi(this));

    }

    public Button getSaveButton() {
        return saveButton;
    }

    public Button getCancelButton() {
        return cancelButton;
    }

    public String getTitle() {
        return titleBox.getText();
    }

    public String getContent() {
        return contentArea.getText();
    }

    public Button getBackButton() {
        return backButton;
    }
}


