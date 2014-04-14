package org.fluxtream.mvc.controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.fluxtream.core.Configuration;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Controller
public class RetinaController {

    @Autowired
    Configuration env;

    @RequestMapping("/*/images/**")
    public void serveImage(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String uri = request.getRequestURI();
        String release = "/" + env.get("release");
        if (uri.startsWith(release))
            uri = uri.substring(release.length());
        else {
            response.sendError(404);
            return;
        }
        setCacheTime(response,31556926);
        serveRetinaImage(uri, request, response);
    }

    @RequestMapping({"/static/img/**", "/static/images/**", "/images/**"})
    public void serveStaticImg(HttpServletRequest request, HttpServletResponse response) throws IOException {
        setCacheTime(response,31556926);
        serveRetinaImage(request.getRequestURI(), request, response);
    }

    private void setCacheTime(HttpServletResponse response, long cacheAge){
        response.setHeader("Cache-Control", "public, max-age="+ cacheAge + ", must-revalidate");
    }

    private void setLastModified(HttpServletResponse response, long lastModified){
        response.setDateHeader("Last-Modified",lastModified);
    }

    private void serveRetinaImage(String path, HttpServletRequest request, HttpServletResponse response) throws IOException {
        final String realPath = request.getSession().getServletContext().getRealPath("");
        path = retinaPath(path, realPath, request);
        String fullPath = realPath + path;
        final File file = new File(fullPath);
        FileInputStream fileinput = null;
        try {
            setLastModified(response,file.lastModified());
            fileinput = new FileInputStream(file);
            IOUtils.copy(fileinput, response.getOutputStream());}
        catch (FileNotFoundException e) { response.sendError(404); }

    }

    private String retinaPath(final String path, final String realPath, final HttpServletRequest request) {
        boolean isRetinaClient = isRetinaClient(request);
        if (isRetinaClient) {
            try {
                int mid= path.lastIndexOf(".");
                String stemPath=path.substring(0,mid);
                String ext=path.substring(mid+1, path.length());
                String retinaPath = (new StringBuilder(stemPath).append("@2x.").append(ext)).toString();
                String fullRetinaPath = ((new StringBuilder(realPath).append(retinaPath)).toString());
                File retinaImageFile = new File(fullRetinaPath);
                if (retinaImageFile.exists())
                    return retinaPath;
            } catch (Exception e) {
                System.out.println(String.format("Path '%s', realPath '%s': couldn't get retina image...", path, realPath));
                e.printStackTrace();
                return path;
            }
        }
        return path;
    }

    private boolean isRetinaClient(final HttpServletRequest request) {
        final Cookie[] cookies = request.getCookies();
        if (cookies==null)
            return false;
        for(int i=0; i<cookies.length; i++) {
            Cookie cookie = cookies[i];
            if ("retina".equals(cookie.getName()))
                return cookie.getValue().equals("1");
        }
        return false;
    }
}
