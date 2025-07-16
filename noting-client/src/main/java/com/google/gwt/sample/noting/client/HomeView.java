package com.google.gwt.sample.noting.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class HomeView extends Composite {

    interface HomeViewUiBinder extends UiBinder<Widget, HomeView> {}
    private static HomeViewUiBinder uiBinder = GWT.create(HomeViewUiBinder.class);

    @UiField
    VerticalPanel notesPanel;

    public HomeView() {
        initWidget(uiBinder.createAndBindUi(this));
    }
    

}