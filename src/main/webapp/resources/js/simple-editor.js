(() => {
    "use strict";
    const allowed = new Set(["P", "BR", "UL", "OL", "LI", "STRONG", "EM", "H3", "H4"]);
    const discard = new Set(["SCRIPT", "STYLE", "IFRAME", "OBJECT", "EMBED", "SVG", "MATH", "IMG", "VIDEO", "AUDIO"]);

    // Client cleanup is for safe editing; the server remains the security boundary.
    function clean(html) {
        const template = document.createElement("template");
        template.innerHTML = html;
        const output = document.createDocumentFragment();
        function copy(node, parent) {
            if (node.nodeType === Node.TEXT_NODE) { parent.append(document.createTextNode(node.textContent)); return; }
            if (node.nodeType !== Node.ELEMENT_NODE || discard.has(node.tagName)) return;
            const name = ({ B: "STRONG", I: "EM", DIV: "P" })[node.tagName] || node.tagName;
            const target = allowed.has(name) ? document.createElement(name.toLowerCase()) : parent;
            if (target !== parent) parent.append(target);
            [...node.childNodes].forEach(child => copy(child, target));
        }
        [...template.content.childNodes].forEach(node => copy(node, output));
        return output;
    }

    document.querySelectorAll("textarea[data-simple-editor]").forEach(source => {
        const editor = document.createElement("div");
        editor.id = `${source.id}-editor`;
        editor.className = "simple-editor";
        editor.contentEditable = "true";
        editor.setAttribute("role", "textbox");
        editor.setAttribute("aria-label", "안내문 내용");
        editor.setAttribute("aria-multiline", "true");
        editor.append(clean(source.value));
        source.dataset.editorTarget = editor.id;
        const toolbar = document.createElement("div");
        toolbar.className = "simple-editor-toolbar";
        toolbar.setAttribute("role", "group");
        toolbar.setAttribute("aria-label", "안내문 서식");
        const sync = () => {
            const holder = document.createElement("div");
            holder.append(clean(editor.innerHTML));
            source.value = holder.innerHTML;
            source.dispatchEvent(new Event("change", { bubbles: true }));
        };
        function insert(fragment) {
            editor.focus();
            const selection = window.getSelection();
            let range = selection.rangeCount ? selection.getRangeAt(0) : null;
            if (!range || !editor.contains(range.commonAncestorContainer)) {
                range = document.createRange();
                range.selectNodeContents(editor);
                range.collapse(false);
            }
            range.deleteContents();
            const last = fragment.lastChild;
            range.insertNode(fragment);
            if (last) range.setStartAfter(last);
            range.collapse(true);
            selection.removeAllRanges();
            selection.addRange(range);
            sync();
        }
        [["굵게", "strong"], ["기울임", "em"], ["문단", "p"], ["목록", "ul"]].forEach(([label, tag]) => {
            const button = document.createElement("button");
            button.type = "button";
            button.className = "secondary-button";
            button.textContent = label;
            button.addEventListener("mousedown", event => event.preventDefault());
            button.addEventListener("click", () => {
                const selection = window.getSelection();
                const selected = selection.rangeCount && editor.contains(selection.getRangeAt(0).commonAncestorContainer)
                    ? selection.toString() : "";
                const element = document.createElement(tag);
                if (tag === "ul") {
                    (selected || "목록 내용").split(/\r?\n/).forEach(line => {
                        const li = document.createElement("li"); li.textContent = line; element.append(li);
                    });
                } else element.textContent = selected || "내용을 입력해 주세요";
                const fragment = document.createDocumentFragment(); fragment.append(element); insert(fragment);
            });
            toolbar.append(button);
        });
        editor.addEventListener("input", sync);
        editor.addEventListener("paste", event => {
            event.preventDefault();
            const html = event.clipboardData.getData("text/html");
            if (html) insert(clean(html));
            else {
                const fragment = document.createDocumentFragment();
                event.clipboardData.getData("text/plain").split(/\r?\n/).forEach(line => {
                    const p = document.createElement("p"); p.textContent = line; fragment.append(p);
                });
                insert(fragment);
            }
        });
        editor.addEventListener("drop", event => event.preventDefault());
        source.hidden = true;
        source.after(toolbar, editor);
        source.form?.addEventListener("submit", sync);
    });
})();
