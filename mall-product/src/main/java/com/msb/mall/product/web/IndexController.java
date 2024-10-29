package com.msb.mall.product.web;

import com.msb.mall.product.entity.CategoryEntity;
import com.msb.mall.product.service.CategoryService;
import com.msb.mall.product.vo.Catalog2VO;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
public class IndexController {

    @Autowired
    private CategoryService categoryService;
    @Autowired
    private RedissonClient redissonClient;

    @GetMapping({"/", "/home", "/index"})
    public String index(Model model) {
        // 查询出所有的一级分类的信息
        List<CategoryEntity> list = categoryService.getLeve1Category();
        model.addAttribute("categorys", list);
        // classPath:/templates/
        // .html
        return "index";
    }

    @ResponseBody
    @RequestMapping("/index/catalog.json")
    public Map<String, List<Catalog2VO>> getCatalog2JSON() {
        Map<String, List<Catalog2VO>> map = categoryService.getCatelog2JSON();
        return map;
    }

    /**
     * 1.锁会自动续期，如果业务时间超长，运行期间Redisson会自动给锁重新添加30s，不用担心业务时间，锁自动过去而造成的数据安全问题
     * 2.加锁的业务只要执行完成， 那么就不会给当前的锁续期，即使我们不去主动的释放锁，锁在默认30s之后也会自动的删除
     *
     * @return
     */
    @ResponseBody
    @GetMapping("/hello")
    public String hello() {
        RLock myLock = redissonClient.getLock("myLock");
        // 加锁
        // myLock.lock();
        // 获取锁，并且给定的过期时间是10s 问题？ 业务如果时间超过了10s，会不会自动续期？
        // 通过效果演示我们可以发现，指定了过期时间后那么自动续期就不会生效了，这时我们就需要注意设置的过期时间一定要满足我们的业务场景
        // 实际开发中我们最好指定过期时间-->性能角度考虑
        myLock.lock();
        try {
            System.out.println("加锁成功...业务处理....." + Thread.currentThread().getName());
            Thread.sleep(30000);
        } catch (Exception e) {

        } finally {
            System.out.println("释放锁成功..." + Thread.currentThread().getName());
            // 释放锁
            myLock.unlock();
        }
        return "hello";
    }
}
