package com.bitauto.ep.fx.webapi.controller;


import com.bitauto.ep.fx.webapi.configure.DynamicRouteServiceImpl;
import com.bitauto.ep.fx.webapi.model.GatewayPredicateDefinition;
import com.bitauto.ep.fx.webapi.model.GatewayRouteDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * 动态路由Controller
 * 注意：对于程序启动时就生成的路由id 不能进行删除，可以修改
 *       新增的路由 可以根据删除
 */
@RestController
@RequestMapping("/route")
public class RouteController {

    @Autowired
    private DynamicRouteServiceImpl dynamicRouteService;

    /* 动态路由json
    {
	"filters": [],
	"id": "shen",
	"order": -11,
	"predicates": [{
		"args": {
			"pattern": "/fallback"
		},
		"name": "Path"
	}],
	"uri": "http://www.baidu.com"
}
*/

    /**
     * 增加路由
     *
     * @param gwdefinition
     * @return
     */
    @PostMapping("/add")
    public String add(@RequestBody GatewayRouteDefinition gwdefinition) {
        try {
            RouteDefinition definition = assembleRouteDefinition(gwdefinition);
            return this.dynamicRouteService.add(definition);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "succss";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable String id) {
        return this.dynamicRouteService.delete(id);
    }

    @PostMapping("/update")
    public String update(@RequestBody GatewayRouteDefinition gwdefinition) {
        try {
            RouteDefinition definition = assembleRouteDefinition(gwdefinition);
            return this.dynamicRouteService.update(definition);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return "faild";
    }

    private RouteDefinition assembleRouteDefinition(GatewayRouteDefinition gwdefinition) throws URISyntaxException {
        RouteDefinition definition = new RouteDefinition();
        List<PredicateDefinition> pdList = new ArrayList<>();
        definition.setId(gwdefinition.getId());
        definition.setOrder(gwdefinition.getOrder());
        List<GatewayPredicateDefinition> gatewayPredicateDefinitionList = gwdefinition.getPredicates();
        for (GatewayPredicateDefinition gpDefinition : gatewayPredicateDefinitionList) {
            PredicateDefinition predicate = new PredicateDefinition();
            predicate.setArgs(gpDefinition.getArgs());
            predicate.setName(gpDefinition.getName());
            pdList.add(predicate);
        }
        definition.setPredicates(pdList);
        URI uri = new URI(gwdefinition.getUri());
        definition.setUri(uri);
        return definition;
    }

}
