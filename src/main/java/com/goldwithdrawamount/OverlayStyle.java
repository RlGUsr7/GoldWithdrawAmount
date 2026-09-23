package com.goldwithdrawamount;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OverlayStyle
{
    PANEL("Panel box"),
    FLOATING_TEXT("Floating text"),
    BOTH("Both"),
    NONE("None");

    private final String name;

    boolean showsPanel()
    {
        return this == PANEL || this == BOTH;
    }

    boolean showsText()
    {
        return this == FLOATING_TEXT || this == BOTH;
    }

    @Override
    public String toString()
    {
        return name;
    }
}