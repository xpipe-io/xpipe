package io.xpipe.app.comp.base;

import io.xpipe.app.comp.SimpleComp;

import javafx.beans.value.ObservableValue;

import lombok.Getter;

public abstract class ModalOverlayContentComp extends SimpleComp {

    @Getter
    protected ModalOverlay modalOverlay;

    void setModalOverlay(ModalOverlay modalOverlay) {
        this.modalOverlay = modalOverlay;
    }

    protected void onClose() {}

    protected ObservableValue<Boolean> busy() {
        return null;
    }
}
