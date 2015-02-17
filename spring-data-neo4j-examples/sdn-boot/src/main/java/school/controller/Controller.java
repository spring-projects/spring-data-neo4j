package school.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import school.controller.exception.NotFoundException;
import school.domain.Entity;
import school.service.Service;

import javax.servlet.http.HttpServletResponse;

@RequestMapping(value = "/api")
public abstract class Controller<T> {

    public void setHeaders(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
        response.setHeader("Expires", "0");
        response.setHeader("Pragma", "no-cache");
    }

    public Iterable<T> list() {
        return getService().findAll();
    }

    public  T create (T entity) {
        return getService().createOrUpdate(entity);
    }

    public T find(Long id) {
        T entity = getService().find(id);
        if (entity != null) {
            System.out.println("from OGM: " + entity);
            return entity;
        }
        throw new NotFoundException();
    }

    public void delete (Long id) {
        if (getService().find(id) != null) {
            getService().delete(id);
        } else {
            throw new NotFoundException();
        }
    }

    public  T update (Long id, T entity) {
        if (getService().find(id) != null) {
            ((Entity)entity).setId(id);
            return getService().createOrUpdate(entity);
        }
        throw new NotFoundException();
    }

    public abstract Service<T> getService();
}
