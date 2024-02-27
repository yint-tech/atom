var docNoticeDiv = document.getElementById("docNotice");
if (docNoticeDiv) {
    let docNotice = window['_docNotice'];
    if (docNotice) {
        docNoticeDiv.innerHTML = docNotice;
    } else {
        const xhr = new XMLHttpRequest();
        const url = "/atom-api/user-info/docNotice";
        xhr.onreadystatechange = function () {
            if (this.readyState === 4 && this.status === 200) {
                window['_docNotice'] = JSON.parse(xhr.responseText).data;
                docNoticeDiv.innerHTML = window['_docNotice'];
            }
        };
        xhr.open("GET", url);
        xhr.send();
    }
}
