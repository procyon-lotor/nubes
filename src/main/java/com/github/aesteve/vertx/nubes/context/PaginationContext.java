package com.github.aesteve.vertx.nubes.context;

import com.github.aesteve.vertx.nubes.handlers.impl.DefaultErrorHandler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.List;

/**
 * Reflects the pagination state for a RoutingContext (request parameters)
 * Provides service methods to :
 * - generate pagination Link headers
 * - generate paginationContext to render a page on the server
 *
 * @author aesteve
 */
public class PaginationContext {

    public static final String DATA_ATTR = "paginationContext";

    private static final String CURRENT_PAGE_QUERY_PARAM = "page";
    private static final String PER_PAGE_QUERY_PARAM = "perPage";
    private static final Integer DEFAULT_PER_PAGE = 30;
    private static final Integer MAX_PER_PAGE = 100;

    private Integer pageAsked = 1;
    private Integer itemsPerPage = DEFAULT_PER_PAGE;
    private Integer totalPages; // Will be set once the request has been processed (payload)

    /**
     * Prefer using fromRoutingContext but can be instanciated directly
     * (for static page generation for instance)
     */
    private PaginationContext(Integer pageAsked, Integer itemsPerPage) {
        if (pageAsked != null) {
            this.pageAsked = pageAsked;
        }
        if (itemsPerPage != null) {
            this.itemsPerPage = itemsPerPage;
        }
    }

    /**
     * The preferred way to create a PaginationContext
     */
    public static PaginationContext fromContext(RoutingContext context) {
        HttpServerRequest request = context.request();
        String pageStr = request.getParam(PaginationContext.CURRENT_PAGE_QUERY_PARAM);
        String perPageStr = request.getParam(PaginationContext.PER_PAGE_QUERY_PARAM);
        Integer page = null;
        Integer perPage = null;
        try {
            if (pageStr != null) {
                page = Integer.parseInt(pageStr);
            }
            if (perPageStr != null) {
                perPage = Integer.parseInt(perPageStr);
            }
        } catch (NumberFormatException e) {
            DefaultErrorHandler.badRequest(context, "Invalid pagination parameters : expecting integers");
        }
        if (perPage != null && perPage > PaginationContext.MAX_PER_PAGE) {
            DefaultErrorHandler.badRequest(context, "Invalid " + PaginationContext.PER_PAGE_QUERY_PARAM + " parameter, max is " + PaginationContext.MAX_PER_PAGE);
        }
        return new PaginationContext(page, perPage);
    }

    public String buildLinkHeader(HttpServerRequest request) {
        List<String> links = getNavLinks(request);
        if (links.isEmpty()) {
            return null;
        }
        return String.join(", ", links);
    }

    private List<String> getNavLinks(HttpServerRequest request) {
        if (totalPages == null) {
            return emptyList();
        }
        List<String> links = new ArrayList<>();
        if (pageAsked > 1) {
            links.add(pageUrl(request, 1, "first"));
            links.add(pageUrl(request, pageAsked - 1, "prev"));
        }
        if (pageAsked < totalPages) {
            links.add(pageUrl(request, totalPages, "last"));
            links.add(pageUrl(request, pageAsked + 1, "next"));
        }
        return links;
    }

    private String pageUrl(HttpServerRequest request, int pageNum, String rel) {
        StringBuilder sb = new StringBuilder("<");
        String url = request.absoluteURI();
        if (!url.contains("?")) { // can't rely on params() 'cause we might have injected some stuff (routing)
            url += "?" + CURRENT_PAGE_QUERY_PARAM + "=" + pageNum;
            url += "&" + PER_PAGE_QUERY_PARAM + "=" + itemsPerPage;
        } else {
            if (url.indexOf(CURRENT_PAGE_QUERY_PARAM + "=") > url.indexOf('?')) {
                url = url.replaceAll(CURRENT_PAGE_QUERY_PARAM + "=([^&]+)", CURRENT_PAGE_QUERY_PARAM + "=" + pageNum);
            } else {
                url += "&" + CURRENT_PAGE_QUERY_PARAM + "=" + pageNum;
            }
            if (!url.contains("&" + PER_PAGE_QUERY_PARAM) && !url.contains("?" + PER_PAGE_QUERY_PARAM)) {
                url += "&" + PER_PAGE_QUERY_PARAM + "=" + itemsPerPage;
            }
        }
        sb.append(url);
        sb.append(">; ");
        sb.append("rel=\"").append(rel).append("\"");
        return sb.toString();
    }

    public Integer getItemsPerPage() {
        return itemsPerPage;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public void setNbItems(Integer nbTotalItems) {
        this.totalPages = nbTotalItems / itemsPerPage;
        if (totalPages == 0) {
            totalPages = 1;
        }
        int modulo = nbTotalItems % itemsPerPage;
        if (modulo > 0) {
            this.totalPages = this.totalPages + 1;
        }
    }

    public void setNbItems(Long nbTotalItems) {
        setNbItems(nbTotalItems.intValue());
    }

    public boolean hasMorePages() {
        return totalPages > pageAsked;
    }

    public int firstItemInPage() {
        return itemsPerPage * (pageAsked - 1);
    }

    public int lastItemInPage() {
        return firstItemInPage() + itemsPerPage;
    }

    /**
     * Creates a JsonObject containing all pagination
     * that can be injected and evaluated
     * when rendering a page on the server
     *
     * @return a JsonObject containing pagination informations
     */
    public JsonObject toJsonObject() {
        JsonObject json = new JsonObject();
        json.put("current", pageAsked);
        json.put("next", pageAsked.equals(totalPages) ? null : pageAsked + 1);
        json.put("last", pageAsked.equals(totalPages) ? null : totalPages);
        json.put("prev", pageAsked.equals(1) ? null : pageAsked - 1);
        json.put("first", pageAsked == 1 ? null : 1);
        json.put("perPage", itemsPerPage);
        json.put("total", totalPages);
        return json;
    }
}
