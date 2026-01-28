package io.xpipe.app.comp.base;

import io.xpipe.app.comp.SimpleRegionBuilder;

import javafx.beans.value.ObservableValue;

import lombok.Getter;

@Getter
public abstract class ModalOverlayContentComp extends SimpleRegionBuilder {

    protected ModalOverlay modalOverlay;

    protected void setModalOverlay(ModalOverlay modalOverlay) {
        this.modalOverlay = modalOverlay;
    }

    protected ObservableValue<Boolean> busy() {
        return null;
    }
}
