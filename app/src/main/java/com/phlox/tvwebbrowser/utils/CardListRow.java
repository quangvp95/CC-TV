/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package com.phlox.tvwebbrowser.utils;

import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ObjectAdapter;

import com.phlox.tvwebbrowser.app.details.ShadowRowPresenterSelector;
import com.phlox.tvwebbrowser.model.cc.CardRow;

/**
 * The {@link CardListRow} allows the {@link ShadowRowPresenterSelector} to access the {@link CardRow}
 * held by the row and determine whether to use a {@link androidx.leanback.widget.Presenter}
 * with or without a shadow.
 */
public class CardListRow extends ListRow {

    private CardRow mCardRow;

    public CardListRow(HeaderItem header, ObjectAdapter adapter, CardRow cardRow) {
        super(header, adapter);
        setCardRow(cardRow);
    }

    public CardRow getCardRow() {
        return mCardRow;
    }

    public void setCardRow(CardRow cardRow) {
        this.mCardRow = cardRow;
    }
}
