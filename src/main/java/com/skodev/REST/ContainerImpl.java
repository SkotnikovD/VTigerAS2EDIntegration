package com.skodev.REST;

import com.skodev.VtigerController.OutboundController;
import com.skodev.exceptions.VtigerInterExc;
import java.io.IOException;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.Status;
import org.simpleframework.http.core.Container;
import org.slf4j.LoggerFactory;

public class ContainerImpl implements Container {

    private final OutboundController controller;

    public ContainerImpl(OutboundController controller) {
        this.controller = controller;
    }

    @Override
    public void handle(Request request, Response response) {
        String id = null;
        try {
            long time = System.currentTimeMillis();
            response.setDate("Date", time);

            if (request.getAddress().getPath().getPath().equals("/VtigerEdiShop/newORDER")) {
                //New Purchase Order notificaton from Vtiger server
                if (request.getMethod().equals("POST")) {
                    id = request.getParameter("id");
                    if (id == null || id.equals("")) {
                        //wrong content
                        response.setStatus(Status.BAD_REQUEST);
                    } else {
                        //Actions when correct id of new PO entity recieved:
                        response.setStatus(Status.OK);
                    }
                } else {
                    response.setStatus(Status.FORBIDDEN);
                }
            } else {
                //Handler for requested resource hasn't been found
                response.setStatus(Status.NOT_FOUND);
                LoggerFactory.getLogger(this.getClass()).warn("Client's request pointed to unexisted resource: {}", request.getAddress());
            }
            response.close();
        } catch (IOException ex) {
            LoggerFactory.getLogger(this.getClass()).warn("IOException in HTTP container handler", ex);
        }
        
        if (id != null && !id.equals("")) {
            LoggerFactory.getLogger(this.getClass()).info("New purchase order with id = {} dedected", id);
            try {
                controller.newOrder(id);
            } catch (VtigerInterExc ex) {
                LoggerFactory.getLogger(this.getClass()).error("New purchase order with id = {} procesing fail", id, ex);
            }
        }
    }
}
