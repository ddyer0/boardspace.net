import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class Hello extends HttpServlet {

    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html");
        PrintWriter out=resp.getWriter();
        out.println("<HTML>");
        out.println("  <HEAD><TITLE>Test Servlet for www.boardspace.net</TITLE></HEAD>");
        out.println("  <BODY bgcolor='#FFFFFF'>");
        out.println("    <H1>Test Servlet for www.boardspace.net</H1>");
        out.println("    <H2>Congratulations, Jakarta-Tomcat is working!");
        out.println("  </BODY>");
        out.println("</HTML>");
        out.close();
    }
}
