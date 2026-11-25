<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="errorCode" value="404"/>
<c:set var="errorTitle" value="Halaman tidak ditemukan"/>
<c:set var="errorMessage" value="Sepertinya halaman yang Anda cari sudah dipindahkan atau tidak pernah ada. Mari kembali mengelola keuangan Anda."/>
<%@ include file="/WEB-INF/views/error/_page.jspf" %>
