/* 上海如静知华信息科技有限公司 https://www.zhuatech.cn/ */
package cn.zhuatech.renewalai;
import org.springframework.stereotype.Component;
import java.util.*;
import java.time.*;
import static cn.zhuatech.renewalai.Model.*;
import static cn.zhuatech.renewalai.Engine.*;

/**
 * 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。
 */
@Component public class Domain {
 private final InsightProvider insight;
 /**
  * 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。
  */
 public Domain(InsightProvider insight){this.insight=insight;}
 /**
  * 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。
  */
 static String text(Row r,String key){return txt(r.data(),key);}
 /**
  * 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。
  */
 public void create(Engine e,User u,String module,Map<String,Object>d){
  switch(module){
   case "customers" -> require(e.all(u,"customers").stream().noneMatch(x->text(x,"name").equalsIgnoreCase(txt(d,"name"))),"客户名称重复");
   case "contracts" -> {e.ref(u,d,"customer","customers");require(date(d,"endDate").isAfter(date(d,"startDate")),"到期日须晚于开始日");require(num(d,"annualValue").stripTrailingZeros().scale()<=2,"年度金额最多两位小数");}
   case "signals" -> {
    Row contract=e.ref(u,d,"contract","contracts");LocalDate at=date(d,"signalDate");
    require(Set.of("ACTIVE","HEALTHY","AT_RISK","PLAN_REVIEW","PLAN_APPROVED").contains(contract.state()),"合同尚未生效或已续签");
    require(!at.isBefore(date(contract.data(),"startDate"))&&!at.isAfter(LocalDate.now()),"信号日期不在有效范围");
    require(e.all(u,"signals").stream().noneMatch(x->text(x,"contract").equals(contract.id())&&text(x,"signalDate").equals(txt(d,"signalDate"))),"同一合同和日期的信号已存在");
    require(num(d,"overdueAmount").stripTrailingZeros().scale()<=2,"逾期金额最多两位小数");
   }
  }
 }
 /**
  * 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。
  */
 public void edit(Engine e,User u,Row r,Map<String,Object>d){
  if(r.module().equals("contracts")){e.ref(u,d,"customer","customers");require(date(d,"endDate").isAfter(date(d,"startDate")),"到期日须晚于开始日");}
 }
 /**
  * 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。
  */
 public String action(Engine e,User u,Row r,String action,Map<String,Object>i,Map<String,Object>d){
  switch(r.module()+"."+action){
   case "contracts.activate" -> {require(!date(d,"startDate").isAfter(LocalDate.now())&&date(d,"endDate").isAfter(LocalDate.now()),"合同有效期不覆盖当前日期");d.put("activatedAt",Instant.now().toString());}
   case "contracts.evaluate" -> {
    var signals=e.all(u,"signals").stream().filter(x->text(x,"contract").equals(r.id())).max(Comparator.comparing(x->date(x.data(),"signalDate"))).orElseThrow(()->new Failure(409,"缺少合同健康信号"));
    var report=insight.assess(r,signals,LocalDate.now());
    e.ledger(u,"insights","RECORDED",Map.of("contract",r.id(),"signal",signals.id(),"score",report.score(),"factors",report.factors(),"recommendations",report.recommendations(),"method","LOCAL_RULES_V1"));
    d.put("riskScore",report.score());d.put("riskFactors",report.factors());d.put("recommendations",report.recommendations());d.put("lastSignal",signals.id());d.put("evaluatedAt",Instant.now().toString());
    return report.score()>=50?"AT_RISK":"HEALTHY";
   }
   case "contracts.plan" -> {LocalDate limit=date(d,"endDate").isBefore(LocalDate.now())?LocalDate.now().plusDays(30):date(d,"endDate");require(!date(i,"actionDue").isBefore(LocalDate.now())&&!date(i,"actionDue").isAfter(limit),"行动截止日超出合同处置窗口");d.put("actionOwner",txt(i,"actionOwner"));d.put("actionDue",txt(i,"actionDue"));d.put("playbook",txt(i,"playbook"));}
   case "contracts.approve" -> {d.put("planApprovedBy",u.username());d.put("planApprovedAt",Instant.now().toString());}
   case "contracts.return" -> d.put("planReturnReason",txt(i,"reason"));
   case "contracts.renew" -> {
    LocalDate next=date(i,"newEndDate");require(next.isAfter(date(d,"endDate")),"新到期日必须晚于原到期日");
    require(num(i,"newAnnualValue").signum()>0&&num(i,"newAnnualValue").stripTrailingZeros().scale()<=2,"续签金额须大于零且最多两位小数");
    e.ledger(u,"renewals","POSTED",Map.of("contract",r.id(),"oldEndDate",txt(d,"endDate"),"newEndDate",next.toString(),"oldAnnualValue",num(d,"annualValue"),"newAnnualValue",num(i,"newAnnualValue"),"approvedBy",u.username(),"approvedAt",Instant.now().toString()));
    d.put("renewedEndDate",next.toString());d.put("renewedAnnualValue",money(num(i,"newAnnualValue")));d.put("renewedBy",u.username());
   }
  }
  return null;
 }
 /**
  * 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。
  */
 public Map<String,Object> metrics(Engine e,User u){return Map.of("风险续约",e.all(u,"contracts").stream().filter(x->x.state().equals("AT_RISK")).count(),"待审计划",e.all(u,"contracts").stream().filter(x->x.state().equals("PLAN_REVIEW")).count(),"已登记续签",e.all(u,"renewals").size());}
}
