<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="errorCode" value="500"/>
<c:set var="errorTitle" value="Terjadi kesalahan"/>
<c:set var="errorMessage" value="Sistem gagal memproses permintaan Anda. Silakan coba beberapa saat lagi."/>
<%@ include file="/WEB-INF/views/error/_page.jspf" %>
