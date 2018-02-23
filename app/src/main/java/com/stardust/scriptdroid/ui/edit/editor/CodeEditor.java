package com.stardust.scriptdroid.ui.edit.editor;

import android.content.Context;
import android.support.design.widget.Snackbar;
import android.util.AttributeSet;

import com.afollestad.materialdialogs.MaterialDialog;
import com.stardust.autojs.script.JsBeautifier;
import com.stardust.scriptdroid.R;
import com.stardust.scriptdroid.ui.edit.theme.Theme;
import com.stardust.util.ClipboardUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.reactivex.Observable;

/**
 * Copyright 2018 WHO<980008027@qq.com>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * Modified by project: https://github.com/980008027/JsDroidEditor
 */
public class CodeEditor extends HVScrollView {


    public interface CursorChangeCallback {

        void onCursorChange(String line, int ch);

    }


    private CharSequence mReplacement = "";
    private String mKeywords;
    private int mFoundIndex = -1;
    private CodeEditText mCodeEditText;
    private TextViewRedoUndo mTextViewRedoUndo;
    private JavaScriptHighlighter mJavaScriptHighlighter;
    private Theme mTheme;
    private JsBeautifier mJsBeautifier;
    private MaterialDialog mProcessDialog;

    public CodeEditor(Context context) {
        super(context);
        init();
    }

    public CodeEditor(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }


    private void init() {
        setFillViewport(true);
        inflate(getContext(), R.layout.code_editor, this);
        mCodeEditText = (CodeEditText) findViewById(R.id.code_edit_text);
        mTextViewRedoUndo = new TextViewRedoUndo(mCodeEditText);
        mJavaScriptHighlighter = new JavaScriptHighlighter(mTheme, mCodeEditText);
        setTheme(Theme.getDefault(getContext()));
        mJsBeautifier = new JsBeautifier(this, "js/beautify.js");

    }

    public Observable<Integer> getLineCount() {
        return Observable.just(mCodeEditText.getLayout().getLineCount());
    }

    public void copyLine() {
        int line = getLineOfChar(mCodeEditText.getSelectionStart());
        if (line < 0 || line >= mCodeEditText.getLayout().getLineCount())
            return;
        CharSequence lineText = mCodeEditText.getText().subSequence(mCodeEditText.getLayout().getLineStart(line),
                mCodeEditText.getLayout().getLineEnd(line));
        ClipboardUtil.setClip(getContext(), lineText);
        Snackbar.make(this, R.string.text_already_copy_to_clip, Snackbar.LENGTH_SHORT).show();
    }

    private int getLineOfChar(int i) {
        int low = 0;
        int high = mCodeEditText.getLineCount() - 1;
        while (low < high) {
            int mid = (low + high) >>> 1;
            int midVal = mCodeEditText.getLayout().getLineEnd(mid);

            if (midVal <= i)
                low = mid + 1;
            else if (midVal > i)
                high = mid - 1;
        }
        return low;
    }

    public void deleteLine() {
        int line = getLineOfChar(mCodeEditText.getSelectionStart());
        if (line < 0 || line >= mCodeEditText.getLayout().getLineCount())
            return;
        mCodeEditText.getText().replace(mCodeEditText.getLayout().getLineStart(line),
                mCodeEditText.getLayout().getLineEnd(line), "");
    }

    public void jumpToStart() {
        mCodeEditText.setSelection(0);
    }

    public void jumpToEnd() {
        mCodeEditText.setSelection(mCodeEditText.getText().length());
    }

    public void jumpToLineStart() {
        int line = getLineOfChar(mCodeEditText.getSelectionStart());
        if (line < 0 || line >= mCodeEditText.getLayout().getLineCount())
            return;
        mCodeEditText.setSelection(mCodeEditText.getLayout().getLineStart(line));
    }

    public void jumpToLineEnd() {
        int line = getLineOfChar(mCodeEditText.getSelectionStart());
        if (line < 0 || line >= mCodeEditText.getLayout().getLineCount())
            return;
        mCodeEditText.setSelection(mCodeEditText.getLayout().getLineEnd(line) - 1);

    }

    public void setTheme(Theme theme) {
        mTheme = theme;
        setBackgroundColor(mTheme.getBackgroundColor());
        mJavaScriptHighlighter.setTheme(theme);
        mJavaScriptHighlighter.updateTokens(mCodeEditText.getText().toString());
        mCodeEditText.setTheme(mTheme);
        invalidate();
    }


    public boolean isTextChanged() {
        return mTextViewRedoUndo.isTextChanged();
    }

    public boolean canUndo() {
        return mTextViewRedoUndo.canUndo();
    }

    public boolean canRedo(){
        return mTextViewRedoUndo.canRedo();
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        mCodeEditText.postInvalidate();
    }

    public CodeEditText getCodeEditText() {
        return mCodeEditText;
    }

    public void setInitialText(String text) {
        mCodeEditText.setText(text);
        mTextViewRedoUndo.setDefaultText(text);
    }

    public void jumpTo(int line, int col) {
        if (line >= mCodeEditText.getLayout().getLineCount() || line < 0) {
            return;
        }
        mCodeEditText.setSelection(mCodeEditText.getLayout().getLineStart(line) + col);
    }

    public void setReadOnly(boolean readOnly) {
        setEnabled(!readOnly);
    }

    public void setProgress(boolean progress) {
        if (progress) {
            if (mProcessDialog != null) {
                mProcessDialog.dismiss();
            }
            mProcessDialog = new MaterialDialog.Builder(getContext())
                    .content(R.string.text_processing)
                    .progress(true, 0)
                    .cancelable(false)
                    .show();
        } else {
            if (mProcessDialog != null) {
                mProcessDialog.dismiss();
                mProcessDialog = null;
            }
        }

    }

    public void setText(String text) {
        mCodeEditText.setText(text);
    }

    public void setCursorChangeCallback(CursorChangeCallback callback) {
        mCodeEditText.setCursorChangeCallback(callback);
    }


    public void undo() {
        mTextViewRedoUndo.undo();
    }

    public void redo() {
        mTextViewRedoUndo.redo();
    }


    public void findNext() {
        Matcher matcher = Pattern.compile(mKeywords).matcher(mCodeEditText.getText());
        if (!matcher.find(mFoundIndex + 1)) {
            return;
        }
        mFoundIndex = matcher.start();
        mCodeEditText.setSelection(mFoundIndex, mFoundIndex + mKeywords.length());
    }

    public void findPrev() {
        // FIXME: 2018/2/21 lastIndexOf不支持正则
        if (mFoundIndex <= 0) {
            mFoundIndex = mCodeEditText.getText().length();
        }
        int index = mCodeEditText.getText().toString().lastIndexOf(mKeywords, mFoundIndex - 1);
        if (index < 0) {
            return;
        }
        mFoundIndex = index;
        mCodeEditText.setSelection(index, index + mKeywords.length());
    }

    public void replaceSelection() {
        mCodeEditText.getText().replace(mCodeEditText.getSelectionStart(), mCodeEditText.getSelectionEnd(), mReplacement);
    }

    public void beautifyCode() {
        setProgress(true);
        mJsBeautifier.beautify(mCodeEditText.getText().toString(), new JsBeautifier.Callback() {
            @Override
            public void onSuccess(String beautifiedCode) {
                setProgress(false);
                mCodeEditText.setText(beautifiedCode);
            }

            @Override
            public void onException(Exception e) {
                setProgress(false);
                e.printStackTrace();
            }
        });
    }

    public void find(String keywords, boolean usingRegex) {
        if (usingRegex) {
            mKeywords = keywords;
        } else {
            mKeywords = Pattern.quote(keywords);
        }
        mFoundIndex = -1;
        findNext();
    }

    public void replace(String keywords, String replacement, boolean usingRegex) {
        mReplacement = replacement == null ? "" : replacement;
        find(keywords, usingRegex);
    }

    public void replaceAll(String keywords, String replacement, boolean usingRegex) {
        if (!usingRegex) {
            keywords = Pattern.quote(keywords);
        }
        String text = mCodeEditText.getText().toString();
        text = text.replaceAll(keywords, replacement);
        setText(text);
    }

    public void insert(String insertText) {
        int selection = Math.max(mCodeEditText.getSelectionStart(), 0);
        mCodeEditText.getText().insert(selection, insertText);
    }

    public void moveCursor(int dCh) {
        mCodeEditText.setSelection(mCodeEditText.getSelectionStart() + dCh);
    }

    public Observable<String> getText() {
        return Observable.just(mCodeEditText.getText().toString());
    }

    public Observable<String> getSelection() {
        int s = mCodeEditText.getSelectionStart();
        int e = mCodeEditText.getSelectionEnd();
        if (s == e) {
            return Observable.just("");
        }
        return Observable.just(mCodeEditText.getText().toString().substring(s, e));
    }


    public void markTextAsSaved() {
        mTextViewRedoUndo.clearHistory();
    }

}
