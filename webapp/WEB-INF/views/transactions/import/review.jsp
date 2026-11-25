<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="f" uri="http://dompetku.app/functions" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<c:set var="pageTitle" value="Pratinjau Import"/>
<c:set var="eyebrow" value="Periksa sebelum menyimpan"/>
<c:set var="active" value="transactions"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>

<section class="page-toolbar">
    <div>
        <a class="back-link" href="${ctx}/app/transactions/import"><ui:icon name="arrow-left"/>Ganti file</a>
        <h2>Pratinjau import</h2>
        <p>${f:esc(preview.message)}</p>
    </div>
</section>

<div class="mini-stat-row three">
    <div class="mini-stat">
        <span class="mini-icon mini-income"><ui:icon name="check"/></span>
        <div><small>Siap diimport</small><strong>${preview.validCount}</strong></div>
    </div>
    <div class="mini-stat">
        <span class="mini-icon mini-warning"><ui:icon name="refresh"/></span>
        <div><small>Duplikat</small><strong>${preview.duplicateCount}</strong></div>
    </div>
    <div class="mini-stat">
        <span class="mini-icon mini-expense"><ui:icon name="alert"/></span>
        <div><small>Bermasalah</small><strong>${preview.errorCount}</strong></div>
    </div>
</div>

<div class="import-layout">
    <article class="card form-card">
        <div class="card-head">
            <div>
                <h2>Pemetaan kolom</h2>
                <p>Nama header tidak harus sama persis; pilih kolom yang sesuai.</p>
            </div>
        </div>
        <form method="post" action="${ctx}/app/transactions/import/preview">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <div class="field-grid three">
                <label class="field">
                    <span>Tanggal <b class="required">*</b></span>
                    <select name="mapDate">
                        <option value="-1">— Pilih kolom —</option>
                        <c:forEach var="col" items="${batch.headers}" varStatus="s">
                            <option value="${s.index}" ${batch.mapping.dateIndex eq s.index ? 'selected' : ''}>${f:esc(col)}</option>
                        </c:forEach>
                    </select>
                </label>
                <label class="field">
                    <span>Nominal <b class="required">*</b></span>
                    <select name="mapAmount">
                        <option value="-1">— Pilih kolom —</option>
                        <c:forEach var="col" items="${batch.headers}" varStatus="s">
                            <option value="${s.index}" ${batch.mapping.amountIndex eq s.index ? 'selected' : ''}>${f:esc(col)}</option>
                        </c:forEach>
                    </select>
                </label>
                <label class="field">
                    <span>Dompet</span>
                    <select name="mapWallet">
                        <option value="-1">— Pakai default —</option>
                        <c:forEach var="col" items="${batch.headers}" varStatus="s">
                            <option value="${s.index}" ${batch.mapping.walletIndex eq s.index ? 'selected' : ''}>${f:esc(col)}</option>
                        </c:forEach>
                    </select>
                </label>
                <label class="field">
                    <span>Jenis</span>
                    <select name="mapType">
                        <option value="-1">— Default pengeluaran —</option>
                        <c:forEach var="col" items="${batch.headers}" varStatus="s">
                            <option value="${s.index}" ${batch.mapping.typeIndex eq s.index ? 'selected' : ''}>${f:esc(col)}</option>
                        </c:forEach>
                    </select>
                </label>
                <label class="field">
                    <span>Kategori</span>
                    <select name="mapCategory">
                        <option value="-1">— Tanpa kategori —</option>
                        <c:forEach var="col" items="${batch.headers}" varStatus="s">
                            <option value="${s.index}" ${batch.mapping.categoryIndex eq s.index ? 'selected' : ''}>${f:esc(col)}</option>
                        </c:forEach>
                    </select>
                </label>
                <label class="field">
                    <span>Catatan</span>
                    <select name="mapNote">
                        <option value="-1">— Tanpa catatan —</option>
                        <c:forEach var="col" items="${batch.headers}" varStatus="s">
                            <option value="${s.index}" ${batch.mapping.noteIndex eq s.index ? 'selected' : ''}>${f:esc(col)}</option>
                        </c:forEach>
                    </select>
                </label>
            </div>
            <div class="field-grid">
                <label class="field">
                    <span>Dompet default</span>
                    <select name="defaultWalletId" required>
                        <c:forEach var="w" items="${wallets}">
                            <option value="${w.id}" ${batch.defaultWalletId eq w.id ? 'selected' : ''}>${f:esc(w.name)}</option>
                        </c:forEach>
                    </select>
                </label>
                <div class="field">
                    <span>Opsi</span>
                    <label class="check-row">
                        <input type="checkbox" name="createCategories" ${batch.createCategories ? 'checked' : ''}>
                        <span>Buat kategori baru jika belum ada</span>
                    </label>
                </div>
            </div>
            <div class="form-actions">
                <a class="btn btn-ghost" href="${ctx}/app/transactions/import">Batal</a>
                <button class="btn btn-ghost" type="submit"><ui:icon name="refresh"/>Terapkan mapping</button>
            </div>
        </form>
    </article>

    <aside class="form-aside">
        <div class="summary-card">
            <p class="eyebrow">File</p>
            <strong>${batch.rowCount} baris</strong>
            <p class="hint">Duplikat dilewati otomatis. Periksa baris bermasalah sebelum konfirmasi.</p>
            <form method="post" action="${ctx}/app/transactions/import/confirm">
                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                <button class="btn btn-primary" type="submit" ${preview.canImport ? '' : 'disabled'}>
                    <ui:icon name="check"/>Import ${preview.validCount} transaksi
                </button>
            </form>
        </div>
    </aside>
</div>

<article class="card table-card import-preview">
    <div class="card-head">
        <div>
            <h2>Contoh baris</h2>
            <p>Menampilkan maksimal 50 baris pertama dari ${batch.rowCount} baris.</p>
        </div>
    </div>
    <c:choose>
        <c:when test="${empty preview.rows}">
            <div class="empty-inline"><ui:icon name="inbox"/>Belum ada baris untuk dipratinjau. Lengkapi pemetaan tanggal dan nominal.</div>
        </c:when>
        <c:otherwise>
            <div class="table-wrap">
                <table class="data-table">
                    <thead>
                    <tr>
                        <th>Baris</th>
                        <th>Tanggal</th>
                        <th>Jenis</th>
                        <th>Kategori</th>
                        <th>Dompet</th>
                        <th>Catatan</th>
                        <th class="right">Nominal</th>
                        <th>Status</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="row" items="${preview.rows}" varStatus="s">
                        <c:if test="${s.index < 50}">
                            <tr>
                                <td><span class="muted">${row.line}</span></td>
                                <td>${f:date(row.date)}</td>
                                <td>${row.type eq 'income' ? 'Masuk' : (empty row.type ? '-' : 'Keluar')}</td>
                                <td>${f:esc(empty row.categoryName ? '-' : row.categoryName)}<c:if test="${row.newCategory}"><span class="soft-badge">baru</span></c:if></td>
                                <td>${f:esc(empty row.walletName ? '-' : row.walletName)}</td>
                                <td>${f:esc(empty row.note ? '-' : row.note)}</td>
                                <td class="right">${f:rupiah(row.amount)}</td>
                                <td>
                                    <c:choose>
                                        <c:when test="${not empty row.error}"><span class="badge-inline danger">${f:esc(row.error)}</span></c:when>
                                        <c:when test="${row.duplicate}"><span class="badge-inline warning">${f:esc(row.duplicateReason)}</span></c:when>
                                        <c:otherwise><span class="badge-inline income">Siap</span></c:otherwise>
                                    </c:choose>
                                </td>
                            </tr>
                        </c:if>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:otherwise>
    </c:choose>
</article>

<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
