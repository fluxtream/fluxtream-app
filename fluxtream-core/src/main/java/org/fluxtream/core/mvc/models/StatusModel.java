package org.fluxtream.core.mvc.models;

import com.google.gson.annotations.Expose;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ApiModel(value = "Generic wrapper object that expose the result of API calls that do not return an explicit value")
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
    @ApiModelProperty(value = "Did it work?", required = true, allowableValues = "OK, KO")
    public String result;

    @Expose
    @ApiModelProperty(value = "User-friendy message", required = false)
    public String message;

    @Expose
    @ApiModelProperty(value = "More info about what happened (e.g. a stack trace)", required = false)
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
