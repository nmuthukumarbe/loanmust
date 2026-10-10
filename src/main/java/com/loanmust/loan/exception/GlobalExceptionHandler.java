package com.loanmust.loan.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Renders a branded error page instead of a stack trace. */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Missing static resources — favicons, browser probes like
     * /.well-known/appspecific/com.chrome.devtools.json, mistyped asset paths. These are
     * routine 404s, not application errors, so they are answered quietly (no stack trace).
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String noStaticResource(NoResourceFoundException ex, HttpServletRequest request, Model model) {
        log.debug("No static resource for {}", request.getRequestURI());
        model.addAttribute("code", "404");
        model.addAttribute("title", "Not found");
        model.addAttribute("message", "That resource does not exist.");
        return "error/custom";
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(Model model) {
        model.addAttribute("code", "404");
        model.addAttribute("title", "Page not found");
        model.addAttribute("message", "The page you asked for does not exist. Check the address or go back to your dashboard.");
        return "error/custom";
    }

    /**
     * The client disconnected (browser closed, navigated away, network dropped, or a slow-client
     * write timed out) while a response was being streamed — most often a large static page under
     * /site. The connection is already gone, so there is nothing to render; answer quietly instead
     * of logging a stack trace and then crashing again trying to write an error page onto the
     * already-committed response. A {@code void} return marks the request handled with no view.
     */
    @ExceptionHandler(org.apache.catalina.connector.ClientAbortException.class)
    public void clientDisconnected(org.apache.catalina.connector.ClientAbortException ex,
                                   HttpServletRequest request) {
        log.debug("Client disconnected while serving {}: {}", request.getRequestURI(),
                ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public String tooLarge(Model model) {
        model.addAttribute("code", "413");
        model.addAttribute("title", "File is too large");
        model.addAttribute("message", "Files must be 15MB or smaller. Compress the document and upload it again.");
        return "error/custom";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String unexpected(Exception ex, HttpServletRequest request, Model model) {
        log.error("Unhandled error at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        model.addAttribute("code", "500");
        model.addAttribute("title", "Something went wrong");
        model.addAttribute("message", "The action could not be completed. Try again, and contact your administrator if it keeps happening.");
        return "error/custom";
    }
}
