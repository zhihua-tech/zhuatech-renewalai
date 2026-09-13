/* 上海如静知华信息科技有限公司 https://www.zhuatech.cn/ */
package cn.zhuatech.renewalai;
import org.springframework.stereotype.Component;
import java.util.*;
import java.time.*;
import static cn.zhuatech.renewalai.Model.*;
import static cn.zhuatech.renewalai.Engine.*;

/** 续约评分接口；默认实现本地可运行，不依赖模型服务。 */
public interface InsightProvider {
 record Insight(int score,List<String> factors,List<String> recommendations){}
 Insight assess(Row contract,Row signal,LocalDate today);
}
@Component class LocalInsightProvider implements InsightProvider {
 public Insight assess(Row contract,Row signal,LocalDate today){
  List<String> factors=new ArrayList<>(),recommendations=new ArrayList<>();int score=0;
  long days=java.time.temporal.ChronoUnit.DAYS.between(today,date(contract.data(),"endDate"));
  if(days<0){score+=100;factors.add("合同已到期");recommendations.add("先核实服务与合同状态，再启动续约处置");}
  else if(days<=num(contract.data(),"noticeDays").longValue()){score+=25;factors.add("合同已进入提前通知窗口");recommendations.add("立即安排续约沟通并确认采购流程");}
  if(num(signal.data(),"utilization").intValue()<50){score+=30;factors.add("席位利用率低于 50%");recommendations.add("与客户核对使用障碍和培训需求");}
  if(num(signal.data(),"openTickets").intValue()>=3){score+=25;factors.add("未结服务工单达到 3 个");recommendations.add("先关闭高优先级服务问题");}
  if(num(signal.data(),"overdueAmount").signum()>0){score+=20;factors.add("存在逾期金额");recommendations.add("与财务核对回款计划");}
  if(factors.isEmpty()){factors.add("未命中本地高风险规则");recommendations.add("按常规节奏跟进续约");}
  return new Insight(Math.min(100,score),List.copyOf(factors),List.copyOf(recommendations));
 }
}
