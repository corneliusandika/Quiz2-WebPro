<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="f" uri="http://dompetku.app/functions" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<c:set var="pageTitle" value="Dompet"/>
<c:set var="eyebrow" value="Semua sumber dana"/>
<c:set var="active" value="wallets"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>

<section class="page-toolbar">
    <div>
        <h2>Dompet &amp; rekening</h2>
        <p>Saldo setiap dompet dihitung otomatis dari saldo awal dan transaksi.</p>
    </div>
    <a class="btn btn-primary" href="${ctx}/app/wallets/new"><ui:icon name="plus"/>Tambah dompet</a>
</section>

<section class="balance-hero">
    <div>
        <span class="balance-label"><ui:icon name="wallet"/>Total saldo gabungan</span>
        <strong class="balance-value">${f:rupiah(totalBalance)}</strong>
        <p>Tersebar di ${items.size()} dompet aktif</p>
    </div>
    <div class="balance-orbs" aria-hidden="true">
        <span class="orb orb-1"></span>
        <span class="orb orb-2"></span>
        <span class="orb orb-3"></span>
    </div>
</section>

<c:choose>
    <c:when test="${empty items}">
        <article class="card">
            <div class="empty-state">
                <span class="empty-icon"><ui:icon name="wallet"/></span>
                <strong>Belum ada dompet</strong>
                <p>Tambahkan dompet tunai, rekening bank, atau e-wallet.</p>
                <a class="btn btn-primary btn-sm" href="${ctx}/app/wallets/new"><ui:icon name="plus"/>Dompet baru</a>
            </div>
        </article>
    </c:when>
    <c:otherwise>
        <div class="wallet-grid">
            <c:forEach var="w" items="${items}">
                <article class="wallet-card" style="--wc:${w.color}">
                    <div class="wallet-top">
                        <span class="wallet-icon">
                            <ui:icon name="${w.type eq 'cash' ? 'cash' : (w.type eq 'bank' ? 'bank' : 'ewallet')}"/>
                        </span>
                        <span class="wallet-type">
                            <c:choose>
                                <c:when test="${w.type eq 'cash'}">Tunai</c:when>
                                <c:when test="${w.type eq 'bank'}">Bank</c:when>
                                <c:otherwise>E-Wallet</c:otherwise>
                            </c:choose>
                        </span>
                        <div class="row-actions">
                            <a class="icon-btn small" href="${ctx}/app/wallets/edit/${w.id}" title="Edit"><ui:icon name="edit"/></a>
                            <form method="post" action="${ctx}/app/wallets/delete"
                                  data-confirm="Hapus dompet ${f:esc(w.name)}? Dompet dengan transaksi tidak dapat dihapus.">
                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="id" value="${w.id}">
                                <button type="submit" class="icon-btn small danger" title="Hapus"><ui:icon name="trash"/></button>
                            </form>
                        </div>
                    </div>
                    <h3>${f:esc(w.name)}</h3>
                    <strong class="wallet-balance">${f:rupiah(w.balance)}</strong>
                    <div class="wallet-bottom">
                        <span>Saldo awal ${f:rupiah(w.initialBalance)}</span>
                        <a href="${ctx}/app/transactions?wallet=${w.id}">Riwayat <ui:icon name="arrow-right"/></a>
                    </div>
                </article>
            </c:forEach>
        </div>
    </c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
