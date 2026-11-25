<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="f" uri="http://dompetku.app/functions" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<c:set var="pageTitle" value="Import CSV"/>
<c:set var="eyebrow" value="Masukkan transaksi dari file"/>
<c:set var="active" value="transactions"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>

<section class="page-toolbar">
    <div>
        <a class="back-link" href="${ctx}/app/transactions"><ui:icon name="arrow-left"/>Kembali ke transaksi</a>
        <h2>Import transaksi dari CSV</h2>
        <p>Unggah file, periksa pemetaan kolom, lalu konfirmasi sebelum data disimpan.</p>
    </div>
</section>

<div class="form-layout">
    <article class="card form-card">
        <form method="post" action="${ctx}/app/transactions/import/preview" enctype="multipart/form-data">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <label class="field">
                <span>File CSV</span>
                <input type="file" name="file" accept=".csv,text/csv" required>
                <small class="hint">Maksimal 2 MB dan 2.000 baris. Encoding UTF-8.</small>
            </label>

            <div class="field-grid">
                <label class="field">
                    <span>Dompet default</span>
                    <select name="defaultWalletId" required>
                        <c:forEach var="w" items="${wallets}">
                            <option value="${w.id}">${f:esc(w.name)}</option>
                        </c:forEach>
                    </select>
                    <small class="hint">Dipakai jika kolom dompet kosong atau tidak dipetakan.</small>
                </label>
                <div class="field">
                    <span>Opsi</span>
                    <label class="check-row">
                        <input type="checkbox" name="createCategories" checked>
                        <span>Buat kategori baru jika belum ada</span>
                    </label>
                </div>
            </div>

            <div class="form-actions">
                <a class="btn btn-ghost" href="${ctx}/app/transactions">Batal</a>
                <button class="btn btn-primary" type="submit"><ui:icon name="upload"/>Baca dan pratinjau</button>
            </div>
        </form>
    </article>

    <aside class="form-aside">
        <div class="summary-card">
            <p class="eyebrow">Format yang didukung</p>
            <p class="hint">Baris pertama harus berupa header. Pemisah koma atau titik koma dideteksi otomatis.</p>
            <pre class="code-sample">Tanggal,Jenis,Kategori,Dompet,Nominal,Catatan
2026-11-18,Pengeluaran,Makan &amp; Minum,GoPay,"45.000",Makan siang
2026-11-18,Pemasukan,Gaji,BCA,"12.000.000",Gaji bulanan</pre>
            <p class="hint">Tanggal: <code>YYYY-MM-DD</code>, <code>DD/MM/YYYY</code>, atau <code>DD-MM-YYYY</code>.</p>
            <p class="hint">Nominal: <code>1.500,50</code>, <code>1500,50</code>, atau <code>1500.50</code>.</p>
        </div>
        <div class="help-card">
            <span class="mini-icon"><ui:icon name="shield"/></span>
            <div><strong>Data tetap terisolasi</strong><p>File diproses untuk akunmu dan tidak dibagikan ke pengguna lain.</p></div>
        </div>
    </aside>
</div>

<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
