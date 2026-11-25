<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="errorCode" value="403"/>
<c:set var="errorTitle" value="Akses ditolak"/>
<c:set var="errorMessage" value="Anda tidak diizinkan membuka halaman ini. Muat ulang halaman atau kembali ke dashboard."/>
<%@ include file="/WEB-INF/views/error/_page.jspf" %>
