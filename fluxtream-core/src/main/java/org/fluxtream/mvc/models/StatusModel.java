package org.fluxtream.mvc.models;

import com.google.gson.annotations.Expose;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StatusModel {

    private static final String SUCCESS = "OK";
    private static final String FAILURE = "KO";

    @NotNull
    public static StatusModel success() {
        return StatusModel.success(null);
    }

    @NotNull
    public static StatusModel success(@Nullable final String message) {
        return StatusModel.success(message, null);
    }

    @NotNull
    public static StatusModel success(@Nullable final String message, @Nullable final Object payload) {
        return new StatusModel(true, message, payload);
    }

    @NotNull
    public static StatusModel failure() {
        return StatusModel.failure(null);
    }

    @NotNull
    public static StatusModel failure(@Nullable final String message) {
        return StatusModel.failure(message, null);
    }

    @NotNull
    public static StatusModel failure(@Nullable final String message, @Nullable final Object payload) {
        return new StatusModel(false, message, payload);
    }

    @Expose
    public String result;

    @Expose
    public String message;

    @Expose
    public Object payload;

    // TODO: make this private, update existing usages to use the static creators above
    public StatusModel(boolean isSuccess, String message) {
        result = isSuccess ? SUCCESS : FAILURE;
        this.message = message;
    }

    private StatusModel(boolean isSuccess, String message, Object payload) {
        result = isSuccess ? SUCCESS : FAILURE;
        this.message = message;
        this.payload = payload;
    }
}
