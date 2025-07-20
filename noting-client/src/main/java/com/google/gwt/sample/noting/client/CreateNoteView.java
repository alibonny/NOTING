package com.google.gwt.sample.noting.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.sample.noting.client.CreateNoteView.CreateNoteViewUiBinder;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class CreateNoteView extends Composite {

    interface CreateNoteViewUiBinder extends UiBinder<Widget, CreateNoteView> {}
    private static CreateNoteViewUiBinder uiBinder = GWT.create(CreateNoteViewUiBinder.class);

    @UiField TextBox titoloBox;
    @UiField TextArea contenutoBox;
   // @UiField Button pubblicaButton;
    //@UiField Button eliminaButton;
    //@UiField Button indietroButton;

    private CreateNoteViewListener listener;

    public void setCreateNoteViewListener(CreateNoteViewListener listener) {
        this.listener = listener;
    }


    public CreateNoteView() {
        initWidget(uiBinder.createAndBindUi(this));

       // pubblicaButton.addClickHandler(e -> {
       //     if (listener != null) {
        //        listener.onPublish(titoloBox.getText(), contenutoBox.getText());
       //     }
       // });
      //  eliminaButton.addClickHandler(e -> {
         //   if (listener != null) {
         //       listener.onDelete();
            }
       /// });
      //  indietroButton.addClickHandler(e -> {
       //     if (listener != null) {
       //         listener.onBack();
       //     }
       // });
    }
    

    

   



