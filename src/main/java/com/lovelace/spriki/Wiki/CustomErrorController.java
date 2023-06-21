package com.lovelace.spriki.Wiki;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class CustomErrorController implements ErrorController {

    Logger logger = LoggerFactory.getLogger(CustomErrorController.class);

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {



        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        logger.warn(status.toString());

        int code;
        if (status != null){
            code = Integer.valueOf(status.toString());
            if (code == HttpStatus.NOT_FOUND.value()) {
                return "404";
            }
            model.addAttribute("errorCode", code);
            logger.warn("Error Code " + code);
        }

        logger.warn("Unknown Error");

        return "error";
    }

}
