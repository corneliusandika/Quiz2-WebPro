<%@ tag body-content="empty" pageEncoding="UTF-8" %>
<%@ attribute name="name" required="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<svg class="icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor"
     stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
    <c:choose>
        <c:when test="${name eq 'home'}"><path d="M3 10.5L12 3l9 7.5"/><path d="M5 9.5V21h14V9.5"/><path d="M9 21v-6h6v6"/></c:when>
        <c:when test="${name eq 'transactions'}"><path d="M4 7h13l-3-3"/><path d="M20 17H7l3 3"/></c:when>
        <c:when test="${name eq 'wallet'}"><rect x="3" y="6" width="18" height="13" rx="3"/><path d="M3 10h18"/><path d="M16 14h2"/></c:when>
        <c:when test="${name eq 'categories'}"><rect x="3" y="3" width="7" height="7" rx="2"/><rect x="14" y="3" width="7" height="7" rx="2"/><rect x="3" y="14" width="7" height="7" rx="2"/><rect x="14" y="14" width="7" height="7" rx="2"/></c:when>
        <c:when test="${name eq 'budget'}"><path d="M21 12a9 9 0 1 1-9-9"/><path d="M12 3v9h9"/></c:when>
        <c:when test="${name eq 'goals'}"><circle cx="12" cy="12" r="9"/><circle cx="12" cy="12" r="5"/><circle cx="12" cy="12" r="1.4" fill="currentColor" stroke="none"/></c:when>
        <c:when test="${name eq 'debts'}"><path d="M7 11V9a3 3 0 0 1 6 0v2"/><path d="M5 11h8v6a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2z"/><circle cx="18" cy="7" r="3"/><path d="M16 14h4v4a2 2 0 0 1-2 2h-2"/></c:when>
        <c:when test="${name eq 'recurring'}"><path d="M17 2l4 4-4 4"/><path d="M3 11V9a4 4 0 0 1 4-4h14"/><path d="M7 22l-4-4 4-4"/><path d="M21 13v2a4 4 0 0 1-4 4H3"/></c:when>
        <c:when test="${name eq 'reports'}"><path d="M4 20V10M10 20V4M16 20v-7M22 20H2"/></c:when>
        <c:when test="${name eq 'user'}"><circle cx="12" cy="8" r="4"/><path d="M4 21a8 8 0 0 1 16 0"/></c:when>
        <c:when test="${name eq 'logout'}"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><path d="M16 17l5-5-5-5"/><path d="M21 12H9"/></c:when>
        <c:when test="${name eq 'plus'}"><path d="M12 5v14M5 12h14"/></c:when>
        <c:when test="${name eq 'edit'}"><path d="M12 20h9"/><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z"/></c:when>
        <c:when test="${name eq 'trash'}"><path d="M3 6h18"/><path d="M8 6V4h8v2"/><path d="M19 6l-1 14H6L5 6"/><path d="M10 11v5M14 11v5"/></c:when>
        <c:when test="${name eq 'search'}"><circle cx="11" cy="11" r="7"/><path d="M20 20l-3.5-3.5"/></c:when>
        <c:when test="${name eq 'filter'}"><path d="M3 5h18l-7 8v6l-4 2v-8z"/></c:when>
        <c:when test="${name eq 'arrow-up'}"><path d="M12 19V5M5 12l7-7 7 7"/></c:when>
        <c:when test="${name eq 'arrow-down'}"><path d="M12 5v14M19 12l-7 7-7-7"/></c:when>
        <c:when test="${name eq 'arrow-right'}"><path d="M5 12h14M12 5l7 7-7 7"/></c:when>
        <c:when test="${name eq 'arrow-left'}"><path d="M19 12H5M12 19l-7-7 7-7"/></c:when>
        <c:when test="${name eq 'chevron-down'}"><path d="M6 9l6 6 6-6"/></c:when>
        <c:when test="${name eq 'chevron-left'}"><path d="M15 6l-6 6 6 6"/></c:when>
        <c:when test="${name eq 'chevron-right'}"><path d="M9 6l6 6-6 6"/></c:when>
        <c:when test="${name eq 'calendar'}"><rect x="3" y="5" width="18" height="16" rx="3"/><path d="M16 3v4M8 3v4M3 11h18"/></c:when>
        <c:when test="${name eq 'check'}"><path d="M20 6L9 17l-5-5"/></c:when>
        <c:when test="${name eq 'x'}"><path d="M18 6L6 18M6 6l12 12"/></c:when>
        <c:when test="${name eq 'menu'}"><path d="M4 6h16M4 12h16M4 18h16"/></c:when>
        <c:when test="${name eq 'sun'}"><circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M2 12h2M20 12h2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M19.1 4.9l-1.4 1.4M6.3 17.7l-1.4 1.4"/></c:when>
        <c:when test="${name eq 'moon'}"><path d="M21 12.8A8.5 8.5 0 1 1 11.2 3a6.5 6.5 0 0 0 9.8 9.8z"/></c:when>
        <c:when test="${name eq 'auto'}"><circle cx="12" cy="12" r="8"/><path d="M12 4a8 8 0 0 1 0 16z" fill="currentColor" stroke="none"/></c:when>
        <c:when test="${name eq 'download'}"><path d="M12 3v12M7 10l5 5 5-5"/><path d="M4 21h16"/></c:when>
        <c:when test="${name eq 'upload'}"><path d="M12 16V4M7 9l5-5 5 5"/><path d="M4 20h16"/></c:when>
        <c:when test="${name eq 'alert'}"><path d="M10.3 3.9L2.2 18a2 2 0 0 0 1.7 3h16.2a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z"/><path d="M12 9v4M12 17h.01"/></c:when>
        <c:when test="${name eq 'clock'}"><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></c:when>
        <c:when test="${name eq 'sparkles'}"><path d="M12 3l1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8z"/><path d="M19 15l.7 2.3L22 18l-2.3.7L19 21l-.7-2.3L16 18l2.3-.7z"/></c:when>
        <c:when test="${name eq 'card'}"><rect x="2" y="5" width="20" height="14" rx="3"/><path d="M2 10h20"/><path d="M6 15h4"/></c:when>
        <c:when test="${name eq 'bank'}"><path d="M3 10l9-6 9 6"/><path d="M5 10v9M9.5 10v9M14.5 10v9M19 10v9"/><path d="M3 21h18"/></c:when>
        <c:when test="${name eq 'cash'}"><rect x="2" y="6" width="20" height="12" rx="2"/><circle cx="12" cy="12" r="3"/><path d="M6 12h.01M18 12h.01"/></c:when>
        <c:when test="${name eq 'ewallet'}"><rect x="6" y="2" width="12" height="20" rx="3"/><path d="M11 18h2"/></c:when>
        <c:when test="${name eq 'utensils'}"><path d="M6 3v18M10 3v6a2 2 0 0 1-4 0V3"/><path d="M17 3c2 2 2 5 0 8v10"/></c:when>
        <c:when test="${name eq 'car'}"><path d="M5 16l1.5-5h11L19 16"/><path d="M4 16h16v3H4z"/><circle cx="7" cy="19" r="1.5"/><circle cx="17" cy="19" r="1.5"/><path d="M6 11l2-4h8l2 4"/></c:when>
        <c:when test="${name eq 'shopping-bag'}"><path d="M6 7h12l1 14H5z"/><path d="M9 7a3 3 0 0 1 6 0"/></c:when>
        <c:when test="${name eq 'receipt'}"><path d="M6 3h12v18l-2-1.5L14 21l-2-1.5L10 21l-2-1.5L6 21z"/><path d="M9 8h6M9 12h6M9 16h3"/></c:when>
        <c:when test="${name eq 'film'}"><rect x="3" y="3" width="18" height="18" rx="2"/><path d="M7 3v18M17 3v18M3 8h4M3 16h4M17 8h4M17 16h4"/></c:when>
        <c:when test="${name eq 'heart-pulse'}"><path d="M20.8 5.6a5.5 5.5 0 0 0-7.8 0L12 6.6l-1-1a5.5 5.5 0 0 0-7.8 7.8L12 22l8.8-8.6a5.5 5.5 0 0 0 0-7.8z"/><path d="M3.5 12h4.5l1.5-3 2.5 6 1.5-3h5.5"/></c:when>
        <c:when test="${name eq 'book'}"><path d="M4 4h11a3 3 0 0 1 3 3v13H7a3 3 0 0 1-3-3z"/><path d="M4 4v13a3 3 0 0 0 3 3h11"/></c:when>
        <c:when test="${name eq 'coffee'}"><path d="M4 8h12v5a5 5 0 0 1-10 0z"/><path d="M16 9h2a3 3 0 0 1 0 6h-2"/><path d="M7 3v2M11 3v2"/></c:when>
        <c:when test="${name eq 'gift'}"><rect x="3" y="8" width="18" height="13" rx="2"/><path d="M12 8v13M3 12h18"/><path d="M12 8S10 3 7.5 4.5 8 8 12 8zM12 8s2-5 4.5-3.5S16 8 12 8z"/></c:when>
        <c:when test="${name eq 'briefcase'}"><rect x="3" y="7" width="18" height="13" rx="2"/><path d="M9 7V4h6v3M3 12h18"/></c:when>
        <c:when test="${name eq 'trending-up'}"><path d="M3 17l6-6 4 4 7-7"/><path d="M15 8h5v5"/></c:when>
        <c:when test="${name eq 'salary'}"><rect x="2" y="6" width="20" height="12" rx="2"/><path d="M6 10h.01M18 14h.01"/><circle cx="12" cy="12" r="2.5"/></c:when>
        <c:when test="${name eq 'tag'}"><path d="M20 13l-7 7-9-9V4h7z"/><circle cx="8" cy="8" r="1.5"/></c:when>
        <c:when test="${name eq 'home-cat'}"><path d="M3 10.5L12 3l9 7.5"/><path d="M5 9.5V21h14V9.5"/></c:when>
        <c:when test="${name eq 'coins'}"><ellipse cx="9" cy="7" rx="6" ry="3"/><path d="M3 7v4c0 1.7 2.7 3 6 3s6-1.3 6-3V7"/><path d="M3 11v4c0 1.7 2.7 3 6 3 1 0 2-.1 2.8-.3"/><ellipse cx="17" cy="15" rx="4" ry="2"/><path d="M13 15v4c0 1.1 1.8 2 4 2s4-.9 4-2v-4"/></c:when>
        <c:when test="${name eq 'zap'}"><path d="M13 2L4 14h6l-1 8 9-12h-6z"/></c:when>
        <c:when test="${name eq 'eye'}"><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7-10-7-10-7z"/><circle cx="12" cy="12" r="3"/></c:when>
        <c:when test="${name eq 'lock'}"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></c:when>
        <c:when test="${name eq 'mail'}"><rect x="3" y="5" width="18" height="14" rx="2"/><path d="M3 7l9 6 9-6"/></c:when>
        <c:when test="${name eq 'shield'}"><path d="M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6z"/><path d="M9 12l2 2 4-4"/></c:when>
        <c:when test="${name eq 'inbox'}"><path d="M4 4h16v16H4z"/><path d="M4 13h4l2 3h4l2-3h4"/></c:when>
        <c:when test="${name eq 'bell'}"><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9"/><path d="M10 21h4"/></c:when>
        <c:when test="${name eq 'refresh'}"><path d="M21 12a9 9 0 1 1-3-6.7L21 8"/><path d="M21 3v5h-5"/></c:when>
        <c:when test="${name eq 'pause'}"><rect x="6" y="4" width="4" height="16" rx="1"/><rect x="14" y="4" width="4" height="16" rx="1"/></c:when>
        <c:when test="${name eq 'play'}"><path d="M6 4l14 8-14 8z"/></c:when>
        <c:when test="${name eq 'more'}"><circle cx="5" cy="12" r="1"/><circle cx="12" cy="12" r="1"/><circle cx="19" cy="12" r="1"/></c:when>
        <c:otherwise><circle cx="12" cy="12" r="9"/><path d="M12 8v4M12 16h.01"/></c:otherwise>
    </c:choose>
</svg>
