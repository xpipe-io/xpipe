package io.xpipe.app.comp.base;

import io.xpipe.app.comp.SimpleComp;

import javafx.beans.value.ObservableValue;

import lombok.Getter;

@Getter
public abstract class ModalOverlayContentComp extends SimpleComp {

    protected ModalOverlay modalOverlay;

    void setModalOverlay(ModalOverlay modalOverlay) {
        this.modalOverlay = modalOverlay;
    }

    protected void onClose() {}

    protected ObservableValue<Boolean> busy() {
        return null;
    }
}
