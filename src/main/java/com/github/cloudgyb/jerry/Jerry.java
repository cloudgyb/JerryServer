package com.github.cloudgyb.jerry;

import com.github.cloudgyb.jerry.http.JerryHttpServer;

import java.io.IOException;
import java.io.InputStream;
//@formatter:off
/**
 * Jerry的主类，负责启动HTTP服务器
 *                             _ooOoo_
 *                            o8888888o
 *                            88" . "88
 *                            (| -_- |)
 *                            O\  =  /O
 *                         ____/`---'\____
 *                       .'  \\|     |//  `.
 *                      /  \\|||  :  |||//  \
 *                     /  _||||| -:- |||||-  \
 *                     |   | \\\  -  /// |   |
 *                     | \_|  ''\---/''  |   |
 *                     \  .-\__  `-`  ___/-. /
 *                   ___`. .'  /--.--\  `. . __
 *                ."" '<  `.___\_<|>_/___.'  >'"".
 *               | | :  `- \`.;`\ _ /`;.`/ - ` : | |
 *               \  \ `-.   \_ __\ /__ _/   .-` /  /
 *          ======`-.____`-.___\_____/___.-`____.-'======
 *                             `=---='
 *          ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
 *                      佛祖保佑       永无BUG
 *            佛曰:
 *                   写字楼里写字间，写字间里程序员；
 *                   程序人员写程序，又拿程序换酒钱。
 *                   酒醒只在网上坐，酒醉还来网下眠。
 *                   酒醉酒醒日复日，网上网下年复年。
 *                   但愿老死电脑间，不愿鞠躬老板前；
 *                   奔驰宝马贵者趣，公交自行程序员。
 *                   别人笑我太疯癫，我笑自己命更贱；
 *                   不见满街漂亮妹，哪来一身好技术。
 */
//@formatter:on
public class Jerry {
    public static void main(String[] args) throws IOException {
        try (InputStream in = Jerry.class.getClassLoader().getResourceAsStream("banner_tiny.txt")) {
            assert in != null;
            byte[] bytes = in.readAllBytes();
            System.out.println(new String(bytes));
        } catch (Exception ignore) {
        }

        JerryHttpServer jerryHttpServer = new JerryHttpServer(8888);
        jerryHttpServer.start();
        Runtime.getRuntime().addShutdownHook(
                new Thread(jerryHttpServer::stop)
        );
    }
}
