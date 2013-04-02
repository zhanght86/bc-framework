package cn.bc.identity.dao.hibernate.jpa;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import cn.bc.core.exception.CoreException;
import cn.bc.core.query.condition.Direction;
import cn.bc.core.query.condition.impl.AndCondition;
import cn.bc.core.query.condition.impl.EqualsCondition;
import cn.bc.core.query.condition.impl.InCondition;
import cn.bc.core.query.condition.impl.OrderCondition;
import cn.bc.db.jdbc.RowMapper;
import cn.bc.identity.dao.ActorDao;
import cn.bc.identity.dao.ActorHistoryDao;
import cn.bc.identity.dao.ActorRelationDao;
import cn.bc.identity.domain.Actor;
import cn.bc.identity.domain.ActorHistory;
import cn.bc.identity.domain.ActorRelation;
import cn.bc.identity.domain.Resource;
import cn.bc.orm.hibernate.jpa.HibernateCrudJpaDao;
import cn.bc.orm.hibernate.jpa.HibernateJpaNativeQuery;

/**
 * 参与者Dao接口的实现
 * 
 * @author dragon
 * 
 */
public class ActorDaoImpl extends HibernateCrudJpaDao<Actor> implements
		ActorDao {
	private static Log logger = LogFactory.getLog(ActorDaoImpl.class);
	private ActorRelationDao actorRelationDao;
	private ActorHistoryDao actorHistoryDao;

	@Autowired
	public void setActorHistoryDao(ActorHistoryDao actorHistoryDao) {
		this.actorHistoryDao = actorHistoryDao;
	}

	@Autowired
	public void setActorRelationDao(ActorRelationDao actorRelationDao) {
		this.actorRelationDao = actorRelationDao;
	}

	public Actor loadByCode(String actorCode) {
		String hql = "from Actor a where a.code=?";
		@SuppressWarnings("rawtypes")
		List all = this.getJpaTemplate().find(hql, actorCode);
		if (all == null || all.isEmpty())
			return null;
		else if (all.size() == 1)
			return (Actor) all.get(0);
		else
			throw new CoreException("return more than one result! actorCode="
					+ actorCode);

		// return this.createQuery().condition(new EqualsCondition("code",
		// actorCode)).singleResult();
	}

	public Actor loadBelong(Long followerId, Integer[] masterTypes) {
		List<Actor> ms = this.findMaster(followerId,
				new Integer[] { ActorRelation.TYPE_BELONG }, masterTypes);
		if (ms != null && !ms.isEmpty()) {
			if (ms.size() > 1) {
				throw new CoreException("no unique for loadBelong!");
			} else {
				return ms.get(0);
			}
		} else {
			return null;
		}
	}

	public List<Actor> findBelong(Long followerId, Integer[] masterTypes) {
		return this.findMaster(followerId,
				new Integer[] { ActorRelation.TYPE_BELONG }, masterTypes);
	}

	@SuppressWarnings("unchecked")
	public List<Actor> findMaster(Long followerId, Integer[] relationTypes,
			Integer[] masterTypes) {
		if (followerId == null)
			return new ArrayList<Actor>();// TODO: 如顶层单位、底层叶子

		ArrayList<Object> args = new ArrayList<Object>();
		StringBuffer hql = new StringBuffer();
		hql.append("select m from Actor m,ActorRelation ar,Actor f");
		hql.append(" where m.id=ar.master.id");
		hql.append(" and f.id=ar.follower.id and f.id=?");
		args.add(followerId);

		// 关联的类型，对应ActorRelation的type属性
		if (relationTypes != null && relationTypes.length > 0) {
			if (relationTypes.length == 1) {
				hql.append(" and ar.type=?");
				args.add(relationTypes[0]);
			} else {
				hql.append(" and ar.type in (?");
				args.add(relationTypes[0]);
				for (int i = 1; i < relationTypes.length; i++) {
					hql.append(",?");
					args.add(relationTypes[i]);
				}
				hql.append(")");
			}
		}

		// 主控方的类型，对应Actor的type属性
		if (masterTypes != null && masterTypes.length > 0) {
			if (masterTypes.length == 1) {
				hql.append(" and m.type=?");
				args.add(masterTypes[0]);
			} else {
				hql.append(" and m.type in (?");
				args.add(masterTypes[0]);
				for (int i = 1; i < masterTypes.length; i++) {
					hql.append(",?");
					args.add(masterTypes[i]);
				}
				hql.append(")");
			}
		}

		// 排序
		hql.append(" order by ar.type,m.type,m.orderNo");
		if (logger.isDebugEnabled()) {
			logger.debug("hql=" + hql.toString());
			logger.debug("args="
					+ StringUtils.collectionToCommaDelimitedString(args));
		}
		return this.getJpaTemplate().find(hql.toString(), args.toArray());
	}

	public List<Actor> findFollower(Long masterId, Integer[] relationTypes,
			Integer[] followerTypes) {
		return this.findFollowerWithName(masterId, null, relationTypes,
				followerTypes, null);
	}

	@SuppressWarnings("unchecked")
	public List<Actor> findFollowerWithName(Long masterId, String followerName,
			Integer[] relationTypes, Integer[] followerTypes,
			Integer[] followerStatuses) {
		if (masterId == null)
			return new ArrayList<Actor>();

		ArrayList<Object> args = new ArrayList<Object>();
		StringBuffer hql = new StringBuffer();
		hql.append("select f from Actor f,ActorRelation ar,Actor m");
		hql.append(" where f.id=ar.follower.id");
		hql.append(" and m.id=ar.master.id and m.id=?");
		args.add(masterId);

		// 关联的类型，对应ActorRelation的type属性
		if (relationTypes != null && relationTypes.length > 0) {
			if (relationTypes.length == 1) {
				hql.append(" and ar.type=?");
				args.add(relationTypes[0]);
			} else {
				hql.append(" and ar.type in (?");
				args.add(relationTypes[0]);
				for (int i = 1; i < relationTypes.length; i++) {
					hql.append(",?");
					args.add(relationTypes[i]);
				}
				hql.append(")");
			}
		}

		// 从属方的类型，对应Actor的type属性
		if (followerTypes != null && followerTypes.length > 0) {
			if (followerTypes.length == 1) {
				hql.append(" and f.type=?");
				args.add(followerTypes[0]);
			} else {
				hql.append(" and f.type in (?");
				args.add(followerTypes[0]);
				for (int i = 1; i < followerTypes.length; i++) {
					hql.append(",?");
					args.add(followerTypes[i]);
				}
				hql.append(")");
			}
		}

		// 从属方的状态，对应Actor的status属性
		if (followerStatuses != null && followerStatuses.length > 0) {
			if (followerStatuses.length == 1) {
				hql.append(" and f.status=?");
				args.add(followerStatuses[0]);
			} else {
				hql.append(" and f.status in (?");
				args.add(followerStatuses[0]);
				for (int i = 1; i < followerStatuses.length; i++) {
					hql.append(",?");
					args.add(followerStatuses[i]);
				}
				hql.append(")");
			}
		}

		// 从属方的名称，对应Actor的name属性
		if (followerName != null && followerName.length() > 0) {
			hql.append(" and f.name=?");
			args.add(followerName);
		}

		// 排序
		hql.append(" order by ar.type,ar.orderNo,f.type,f.orderNo");
		if (logger.isDebugEnabled()) {
			logger.debug("hql=" + hql.toString());
			logger.debug("args="
					+ StringUtils.collectionToCommaDelimitedString(args));
		}
		return this.getJpaTemplate().find(hql.toString(), args.toArray());
	}

	public List<Actor> findLowerOrganization(Long higherOrganizationId,
			Integer... lowerOrganizationTypes) {
		// 默认为单位+部门+岗位
		if (lowerOrganizationTypes == null)
			lowerOrganizationTypes = new Integer[] { Actor.TYPE_UNIT,
					Actor.TYPE_DEPARTMENT, Actor.TYPE_GROUP };
		return this.findFollower(higherOrganizationId,
				new Integer[] { ActorRelation.TYPE_BELONG },
				lowerOrganizationTypes);
	}

	public List<Actor> findHigherOrganization(Long lowerOrganizationId,
			Integer... higherOrganizationTypes) {
		// 默认为单位+部门+岗位
		if (higherOrganizationTypes == null)
			higherOrganizationTypes = new Integer[] { Actor.TYPE_UNIT,
					Actor.TYPE_DEPARTMENT, Actor.TYPE_GROUP };
		return this.findMaster(lowerOrganizationId,
				new Integer[] { ActorRelation.TYPE_BELONG },
				higherOrganizationTypes);
	}

	@SuppressWarnings("unchecked")
	public List<Actor> findTopUnit() {
		ArrayList<Object> args = new ArrayList<Object>();
		StringBuffer hql = new StringBuffer();
		hql.append("select m from Actor m where m.type=? and m.id not in (");
		args.add(Actor.TYPE_UNIT);
		hql.append("select f.id from Actor f,ActorRelation ar");
		hql.append(" where f.id=ar.follower.id");
		hql.append(" and f.type=? and ar.type=?");
		args.add(Actor.TYPE_UNIT);
		args.add(ActorRelation.TYPE_BELONG);
		hql.append(")");

		// 排序
		hql.append(" order by m.orderNo");
		if (logger.isDebugEnabled()) {
			logger.debug("hql=" + hql.toString());
			logger.debug("args="
					+ StringUtils.collectionToCommaDelimitedString(args));
		}
		return this.getJpaTemplate().find(hql.toString(), args.toArray());
	}

	public List<Actor> findAllUnit(Integer... statues) {
		AndCondition c = new AndCondition();
		c.add(new EqualsCondition("type", new Integer(Actor.TYPE_UNIT))).add(
				new OrderCondition("orderNo", Direction.Asc).add("code",
						Direction.Asc));
		if (statues != null && statues.length > 0) {
			if (statues.length == 1) {
				c.add(new EqualsCondition("status", statues[0]));
			} else {
				c.add(new InCondition("status", statues));
			}
		}
		return this.createQuery().condition(c).list();
	}

	public List<Actor> findUser(Long organizationId) {
		return this.findFollower(organizationId,
				new Integer[] { ActorRelation.TYPE_BELONG },
				new Integer[] { Actor.TYPE_USER });
	}
	
	public List<Actor> findUser(Long organizationId,Integer[] statuses) {
		return this.findFollowerWithName(organizationId,null,
				new Integer[] { ActorRelation.TYPE_BELONG },
				new Integer[] { Actor.TYPE_USER },statuses);
	}

	public List<Actor> findAncestorOrganization(Long lowerOrganizationId,
			Integer... ancestorOrganizationTypes) {
		// 默认为单位+部门+岗位
		if (ancestorOrganizationTypes == null
				|| ancestorOrganizationTypes.length == 0)
			ancestorOrganizationTypes = new Integer[] { Actor.TYPE_UNIT,
					Actor.TYPE_DEPARTMENT, Actor.TYPE_GROUP };

		// TODO 性能优化，以下只是使用了递归查找
		Set<Actor> ancestors = new LinkedHashSet<Actor>();// 使用Set避免重复
		this.recursiveFindHigherOrganization(ancestors, lowerOrganizationId,
				ancestorOrganizationTypes);
		return new ArrayList<Actor>(ancestors);
	}

	// 递归查找祖先组织
	private void recursiveFindHigherOrganization(Set<Actor> ancestors,
			Long lowerId, Integer... ancestorOrganizationTypes) {
		List<Actor> highers = this.findHigherOrganization(lowerId,
				ancestorOrganizationTypes);
		if (highers != null && !highers.isEmpty()) {
			for (Actor higher : highers) {
				this.recursiveFindHigherOrganization(ancestors, higher.getId(),
						ancestorOrganizationTypes);
				ancestors.add(higher);
			}
		}
	}

	public List<Actor> findDescendantOrganization(Long higherOrganizationId,
			Integer... descendantOrganizationTypes) {
		// 默认为单位+部门+岗位
		if (descendantOrganizationTypes == null)
			descendantOrganizationTypes = new Integer[] { Actor.TYPE_UNIT,
					Actor.TYPE_DEPARTMENT, Actor.TYPE_GROUP };

		// TODO 性能优化，以下只是使用了递归查找
		List<Actor> descendants = new ArrayList<Actor>();
		this.recursiveFindDescendantOrganization(descendants,
				higherOrganizationId, descendantOrganizationTypes);
		return descendants;
	}

	// 递归查找后代组织
	private void recursiveFindDescendantOrganization(List<Actor> descendants,
			Long higherOrganizationId, Integer[] descendantOrganizationTypes) {
		List<Actor> lowers = this.findLowerOrganization(higherOrganizationId,
				descendantOrganizationTypes);
		if (lowers != null && !lowers.isEmpty()) {
			for (Actor lower : lowers) {
				descendants.add(lower);
				this.recursiveFindDescendantOrganization(descendants,
						lower.getId(), descendantOrganizationTypes);
			}
		}
	}

	public List<Actor> findDescendantUser(Long organizationId,
			Integer... descendantOrganizationTypes) {
		// 查找直接隶属的人员信息
		List<Actor> users = new ArrayList<Actor>();
		List<Actor> _users = this.findUser(organizationId);
		if (_users != null && !_users.isEmpty()) {
			users.addAll(_users);
		}

		// 获取所有后代组织
		List<Actor> descendantOrganizations = this.findDescendantOrganization(
				organizationId, descendantOrganizationTypes);

		// 循环每个组织查找人员信息
		if (descendantOrganizations != null
				&& !descendantOrganizations.isEmpty()) {
			for (Actor org : descendantOrganizations) {
				_users = this.findUser(org.getId());
				if (_users != null && !_users.isEmpty()) {
					users.addAll(_users);
				}
			}
		}
		return users;
	}
	
	public List<Actor> findDescendantUser(Long organizationId,Integer[] statuses,
			Integer... descendantOrganizationTypes) {
		// 查找直接隶属的人员信息
		List<Actor> users = new ArrayList<Actor>();
		List<Actor> _users = this.findUser(organizationId,statuses);
		if (_users != null && !_users.isEmpty()) {
			users.addAll(_users);
		}

		// 获取所有后代组织
		List<Actor> descendantOrganizations = this.findDescendantOrganization(
				organizationId, descendantOrganizationTypes);

		// 循环每个组织查找人员信息
		if (descendantOrganizations != null
				&& !descendantOrganizations.isEmpty()) {
			for (Actor org : descendantOrganizations) {
				_users = this.findUser(org.getId(),statuses);
				if (_users != null && !_users.isEmpty()) {
					users.addAll(_users);
				}
			}
		}
		return users;
	}

	public List<Resource> findCanUseModules(Long actorId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void delete(Serializable pk) {
		// 先删除关联关系
		if (pk != null)
			this.actorRelationDao
					.deleteByMasterOrFollower(new Serializable[] { pk });
		// 再删除自身
		super.delete(pk);
	}

	@Override
	public void delete(Serializable[] pks) {
		// 先删除关联关系
		this.actorRelationDao.deleteByMasterOrFollower(pks);
		// 再删除自身
		super.delete(pks);
	}

	public Actor save4belong(Actor follower, Long belongId) {
		Long[] belongs = (belongId == null ? null : new Long[] { belongId });
		return this.save4belong(follower, belongs);
	}

	public Actor save4belong(Actor follower, Long[] belongIds) {
		Calendar now = Calendar.getInstance();
		// 调用基类的保存，获取id值
		follower = this.save(follower);

		// 获取原来隶属的上级（单位或部门）
		List<ActorRelation> oldArs = this.actorRelationDao.findByFollower(
				ActorRelation.TYPE_BELONG, follower.getId(), new Integer[] {
						Actor.TYPE_UNIT, Actor.TYPE_DEPARTMENT });
		if (belongIds != null && belongIds.length > 0) {
			List<Actor> sameBelongs = new ArrayList<Actor>();// 没有改变的belong
			List<Actor> newBelongs = new ArrayList<Actor>();// 新加的belong

			// 重新加载belongs
			Actor[] belongs = new Actor[belongIds.length];
			boolean same = false;
			for (int i = 0; i < belongIds.length; i++) {
				belongs[i] = this.load(belongIds[i]);
				for (ActorRelation oldAr : oldArs) {
					if (oldAr.getMaster().getId().equals(belongIds[i])) {
						same = true;
						break;
					}
				}
				if (same) {
					sameBelongs.add(belongs[i]);
				} else {
					newBelongs.add(belongs[i]);
				}
			}

			// 删除不再存在的隶属关系
			if (!oldArs.isEmpty() && sameBelongs.size() != oldArs.size()) {
				List<ActorRelation> toDeleteArs = new ArrayList<ActorRelation>();
				Long mid;
				for (ActorRelation oldAr : oldArs) {
					mid = oldAr.getMaster().getId();
					for (Actor belong : sameBelongs) {
						same = false;
						if (mid.equals(belong.getId())) {
							same = true;
							break;
						}
					}
					if (!same) {
						toDeleteArs.add(oldAr);
					}
				}
				if (!toDeleteArs.isEmpty()) {
					// 删除隶属关系
					this.actorRelationDao.delete(toDeleteArs);

					// 处理隶属关系的历史信息：current设为false，endDate设为当前时间
					this.updateActorHistory4delete(follower, toDeleteArs, now);
				}
			}

			// 创建新的隶属关系
			if (!newBelongs.isEmpty()) {
				List<ActorRelation> newArs = new ArrayList<ActorRelation>();
				ActorRelation newAr;
				for (Actor belong : newBelongs) {
					newAr = new ActorRelation();
					newAr.setFollower(follower);
					newAr.setMaster(belong);
					newAr.setType(ActorRelation.TYPE_BELONG);
					newArs.add(newAr);
				}
				this.actorRelationDao.save(newArs);

				// 创建隶属关系历史信息
				this.createActorHistory(follower, newBelongs, now);
			}

			// 根据新的隶属关系重新设置pcode、pname
			// if (sameBelongs.size() != oldArs.size() || !newBelongs.isEmpty())
			// {
			List<String> pcodes = new ArrayList<String>();
			List<String> pnames = new ArrayList<String>();
			for (Actor belong : sameBelongs) {
				pcodes.add(belong.getFullCode());
				pnames.add(belong.getFullName());
			}
			for (Actor belong : newBelongs) {
				pcodes.add(belong.getFullCode());
				pnames.add(belong.getFullName());
			}
			follower.setPcode(StringUtils
					.collectionToCommaDelimitedString(pcodes));
			follower.setPname(StringUtils
					.collectionToCommaDelimitedString(pnames));
			follower = this.save(follower);
			// }
		} else {// 删除所有现存的隶属关系
			if (!oldArs.isEmpty())
				this.actorRelationDao.delete(oldArs);

			// 没有隶属关系了就设置pcode、pname为空
			follower.setPcode(null);
			follower.setPname(null);
			follower = this.save(follower);
		}

		return follower;
	}

	// 创建隶属关系历史信息
	private void createActorHistory(Actor follower, List<Actor> newBelongs,
			Calendar now) {
		ActorHistory history;
		List<ActorHistory> histories = new ArrayList<ActorHistory>();
		Long oldId;
		for (Actor belong : newBelongs) {
			history = new ActorHistory();

			// TODO 多个隶属关系的处理
			oldId = this.updateActorHistory4new(follower, now);
			if (oldId != null) {
				history.setStartDate(now);
				history.setPid(oldId);
			} else {
				history.setStartDate(null);
				history.setPid(null);
			}

			histories.add(history);
			history.setActorId(follower.getId());
			history.setActorType(follower.getType());
			history.setName(follower.getName());
			history.setCode(follower.getCode());
			history.setCreateDate(now);
			history.setCurrent(true);
			history.setRank(0);

			// 设置直属上级信息
			history.setUpperId(belong.getId());
			history.setUpperName(belong.getName());
			history.setPcode(belong.getFullCode());
			history.setPname(belong.getFullName());

			// TODO 设置所属单位信息
			if (belong.getType() != Actor.TYPE_UNIT) {
				Actor unit = this.loadBelong(belong.getId(), new Integer[] {
						Actor.TYPE_UNIT, Actor.TYPE_DEPARTMENT });
				if (unit != null) {
					history.setUnitId(unit.getId());
					history.setUnitName(unit.getName());
				} else {
					throw new CoreException("没有找到隶属的组织信息：follower="
							+ belong.getId() + "|" + follower.getName());
				}
			} else {
				history.setUnitId(belong.getId());
				history.setUnitName(belong.getName());
			}
		}

		if (!histories.isEmpty()) {
			this.actorHistoryDao.save(histories);
		}
	}

	// 处理隶属关系的历史信息：current设为false，endDate设为当前时间，返回旧记录的id
	private Long updateActorHistory4new(Actor follower, Calendar endDate) {
		// TODO 多个隶属关系的处理
		ActorHistory actorHistory = this.actorHistoryDao.loadCurrent(follower
				.getId());
		if (actorHistory != null) {
			actorHistory.setCurrent(false);
			actorHistory.setEndDate(endDate);
			this.actorHistoryDao.save(actorHistory);
			return actorHistory.getId();
		} else {
			return null;
		}
	}

	// 处理隶属关系的历史信息：current设为false，endDate设为当前时间
	private void updateActorHistory4delete(Actor follower,
			List<ActorRelation> toDeleteArs, Calendar endDate) {
		// TODO 多个隶属关系的处理
		ActorHistory actorHistory = this.actorHistoryDao.loadCurrent(follower
				.getId());
		if (actorHistory != null) {
			actorHistory.setCurrent(false);
			actorHistory.setEndDate(endDate);
			this.actorHistoryDao.save(actorHistory);
		} else {
			String msg = "have no actorHistory: actor.id=" + follower.getId()
					+ ",type=" + follower.getName();
			logger.warn(msg);
			throw new CoreException(msg);
		}
	}

	@SuppressWarnings("unchecked")
	public List<Actor> find(Integer[] actorTypes, Integer[] actorStatues) {
		ArrayList<Object> args = new ArrayList<Object>();
		StringBuffer hql = new StringBuffer();
		hql.append("from Actor a");

		boolean isWhere = true;
		// 类型
		if (actorTypes != null && actorTypes.length > 0) {
			isWhere = false;
			if (actorTypes.length == 1) {
				hql.append(" where a.type=?");
				args.add(actorTypes[0]);
			} else {
				hql.append(" where a.type in (?");
				args.add(actorTypes[0]);
				for (int i = 1; i < actorTypes.length; i++) {
					hql.append(",?");
					args.add(actorTypes[i]);
				}
				hql.append(")");
			}
		}

		// 状态
		if (actorStatues != null && actorStatues.length > 0) {
			if (actorStatues.length == 1) {
				hql.append(" " + (isWhere ? "where" : "and") + " a.status=?");
				args.add(actorStatues[0]);
			} else {
				hql.append(" " + (isWhere ? "where" : "and")
						+ " a.status in (?");
				args.add(actorStatues[0]);
				for (int i = 1; i < actorStatues.length; i++) {
					hql.append(",?");
					args.add(actorStatues[i]);
				}
				hql.append(")");
			}
		}

		// 排序
		hql.append(" order by a.orderNo");
		if (logger.isDebugEnabled()) {
			logger.debug("hql=" + hql.toString());
			logger.debug("args="
					+ StringUtils.collectionToCommaDelimitedString(args));
		}
		return this.getJpaTemplate().find(hql.toString(), args.toArray());
	}

	@SuppressWarnings("unchecked")
	public List<ActorHistory> findHistory(Integer[] actorTypes,
			Integer[] actorStatues) {
		ArrayList<Object> args = new ArrayList<Object>();
		StringBuffer hql = new StringBuffer();
		hql.append("select ah from ActorHistory ah,Actor a where ah.actorId = a.id");

		// 类型
		if (actorTypes != null && actorTypes.length > 0) {
			if (actorTypes.length == 1) {
				hql.append(" and a.type=?");
				args.add(actorTypes[0]);
			} else {
				hql.append(" and a.type in (?");
				args.add(actorTypes[0]);
				for (int i = 1; i < actorTypes.length; i++) {
					hql.append(",?");
					args.add(actorTypes[i]);
				}
				hql.append(")");
			}
		}

		// 状态
		if (actorStatues != null && actorStatues.length > 0) {
			if (actorStatues.length == 1) {
				hql.append(" and a.status=?");
				args.add(actorStatues[0]);
			} else {
				hql.append(" and a.status in (?");
				args.add(actorStatues[0]);
				for (int i = 1; i < actorStatues.length; i++) {
					hql.append(",?");
					args.add(actorStatues[i]);
				}
				hql.append(")");
			}
		}

		// 排序
		hql.append(" order by a.orderNo");
		if (logger.isDebugEnabled()) {
			logger.debug("hql=" + hql.toString());
			logger.debug("args="
					+ StringUtils.collectionToCommaDelimitedString(args));
		}
		return this.getJpaTemplate().find(hql.toString(), args.toArray());
	}

	public List<Map<String, String>> find4option(Integer[] actorTypes,
			Integer[] actorStatues) {
		ArrayList<Object> args = new ArrayList<Object>();
		StringBuffer hql = new StringBuffer();
		hql.append("select a.id,a.type_,a.code,a.name,a.pcode,a.pname");
		hql.append(" from BC_IDENTITY_ACTOR a");

		boolean isWhere = true;
		// 类型
		if (actorTypes != null && actorTypes.length > 0) {
			isWhere = false;
			if (actorTypes.length == 1) {
				hql.append(" where a.type_=?");
				args.add(actorTypes[0]);
			} else {
				hql.append(" where a.type_ in (?");
				args.add(actorTypes[0]);
				for (int i = 1; i < actorTypes.length; i++) {
					hql.append(",?");
					args.add(actorTypes[i]);
				}
				hql.append(")");
			}
		}

		// 状态
		if (actorStatues != null && actorStatues.length > 0) {
			if (actorStatues.length == 1) {
				hql.append((isWhere ? " where" : " and") + " a.status_=?");
				args.add(actorStatues[0]);
			} else {
				hql.append((isWhere ? " where" : " and") + " a.status_ in (?");
				args.add(actorStatues[0]);
				for (int i = 1; i < actorStatues.length; i++) {
					hql.append(",?");
					args.add(actorStatues[i]);
				}
				hql.append(")");
			}
		}

		// 排序
		hql.append(" order by a.order_");
		if (logger.isDebugEnabled()) {
			logger.debug("args="
					+ StringUtils.collectionToCommaDelimitedString(args)
					+ ";hql=" + hql.toString());
		}

		return HibernateJpaNativeQuery.executeNativeSql(getJpaTemplate(),
				hql.toString(), args.toArray(),
				new RowMapper<Map<String, String>>() {
					public Map<String, String> mapRow(Object[] rs, int rowNum) {
						Map<String, String> map = new HashMap<String, String>();
						int i = 0;
						map.put("id", rs[i++].toString());
						map.put("type_", rs[i++].toString());
						map.put("type", map.get("type"));
						map.put("code", rs[i++].toString());
						map.put("name", rs[i++].toString());
						map.put("pcode", rs[i] != null ? rs[i].toString() : "");
						i++;
						map.put("pname", rs[i] != null ? rs[i].toString() : "");
						return map;
					}
				});
	}

	public List<Map<String, String>> findHistory4option(Integer[] actorTypes,
			Integer[] actorStatues) {
		ArrayList<Object> args = new ArrayList<Object>();
		StringBuffer hql = new StringBuffer();
		hql.append("select h.id as id,h.actor_id as aid,a.type_ as type");
		hql.append(",a.code as code,h.actor_name as name,h.pcode as pcode,h.pname as pname");
		hql.append(" from bc_identity_actor_history h");
		hql.append(" inner join bc_identity_actor a on a.id=h.actor_id");
		hql.append(" where h.current = ?");
		args.add(new Boolean(true));

		// 类型
		if (actorTypes != null && actorTypes.length > 0) {
			if (actorTypes.length == 1) {
				hql.append(" and a.type_=?");
				args.add(actorTypes[0]);
			} else {
				hql.append(" and a.type_ in (?");
				args.add(actorTypes[0]);
				for (int i = 1; i < actorTypes.length; i++) {
					hql.append(",?");
					args.add(actorTypes[i]);
				}
				hql.append(")");
			}
		}

		// 状态
		if (actorStatues != null && actorStatues.length > 0) {
			if (actorStatues.length == 1) {
				hql.append(" and a.status_=?");
				args.add(actorStatues[0]);
			} else {
				hql.append(" and a.status_ in (?");
				args.add(actorStatues[0]);
				for (int i = 1; i < actorStatues.length; i++) {
					hql.append(",?");
					args.add(actorStatues[i]);
				}
				hql.append(")");
			}
		}

		// 排序
		hql.append(" order by a.order_,h.rank");
		if (logger.isDebugEnabled()) {
			logger.debug("args="
					+ StringUtils.collectionToCommaDelimitedString(args)
					+ ";hql=" + hql.toString());
		}

		return HibernateJpaNativeQuery.executeNativeSql(getJpaTemplate(),
				hql.toString(), args.toArray(),
				new RowMapper<Map<String, String>>() {
					public Map<String, String> mapRow(Object[] rs, int rowNum) {
						Map<String, String> map = new HashMap<String, String>();
						int i = 0;
						map.put("id", rs[i++].toString());
						map.put("aid", rs[i++].toString());
						map.put("type_", rs[i++].toString());
						map.put("type", map.get("type"));
						map.put("code", rs[i++].toString());
						map.put("name", rs[i++].toString());
						map.put("pcode", rs[i] != null ? rs[i].toString() : "");
						i++;
						map.put("pname", rs[i] != null ? rs[i].toString() : "");
						return map;
					}
				});
	}

	public String loadActorNameByCode(String actorCode) {
		if (actorCode == null || actorCode.length() == 0)
			return null;
		ArrayList<Object> args = new ArrayList<Object>();
		StringBuffer hql = new StringBuffer();
		hql.append("select name from bc_identity_actor where code = ?");
		args.add(actorCode);
		if (logger.isDebugEnabled()) {
			logger.debug("args="
					+ StringUtils.collectionToCommaDelimitedString(args)
					+ ";hql=" + hql.toString());
		}
		List<String> r = HibernateJpaNativeQuery.executeNativeSql(
				getJpaTemplate(), hql.toString(), args.toArray(), null);

		if (r == null || r.isEmpty())
			return actorCode;// 找不到就返回原始的帐号信息
		else
			return r.get(0);
	}

	public String loadActorFullNameByCode(String actorCode) {
		if (actorCode == null || actorCode.length() == 0)
			return null;
		ArrayList<Object> args = new ArrayList<Object>();
		StringBuffer hql = new StringBuffer();
		hql.append("select name,pname from bc_identity_actor where code = ?");
		args.add(actorCode);
		if (logger.isDebugEnabled()) {
			logger.debug("args="
					+ StringUtils.collectionToCommaDelimitedString(args)
					+ ";hql=" + hql.toString());
		}
		List<String> r = HibernateJpaNativeQuery.executeNativeSql(
				getJpaTemplate(), hql.toString(), args.toArray(),
				new RowMapper<String>() {
					public String mapRow(Object[] rs, int rowNum) {
						int i = 0;
						Object name = rs[i++];
						Object pname = rs[i++];
						return pname == null ? name.toString() : pname
								.toString() + "/" + name.toString();
					}
				});
		if (r == null || r.isEmpty())
			return actorCode;// 找不到就返回原始的帐号信息
		else
			return r.get(0);
	}

	@SuppressWarnings("unchecked")
	public List<Actor> findByName(String actorName, Integer[] actorTypes,
			Integer[] actorStatuses) {
		if (actorName == null)
			return new ArrayList<Actor>();

		ArrayList<Object> args = new ArrayList<Object>();
		StringBuffer hql = new StringBuffer();
		hql.append("from Actor a where a.name=?");
		args.add(actorName);

		// 类型，对应Actor的type属性
		if (actorTypes != null && actorTypes.length > 0) {
			if (actorTypes.length == 1) {
				hql.append(" and a.type=?");
				args.add(actorTypes[0]);
			} else {
				hql.append(" and a.type in (?");
				args.add(actorTypes[0]);
				for (int i = 1; i < actorTypes.length; i++) {
					hql.append(",?");
					args.add(actorTypes[i]);
				}
				hql.append(")");
			}
		}

		// 状态，对应Actor的status属性
		if (actorStatuses != null && actorStatuses.length > 0) {
			if (actorStatuses.length == 1) {
				hql.append(" and a.status=?");
				args.add(actorStatuses[0]);
			} else {
				hql.append(" and a.status in (?");
				args.add(actorStatuses[0]);
				for (int i = 1; i < actorStatuses.length; i++) {
					hql.append(",?");
					args.add(actorStatuses[i]);
				}
				hql.append(")");
			}
		}

		// 排序
		hql.append(" order by a.type,a.orderNo");
		if (logger.isDebugEnabled()) {
			logger.debug("hql=" + hql.toString());
			logger.debug("args="
					+ StringUtils.collectionToCommaDelimitedString(args));
		}
		return this.getJpaTemplate().find(hql.toString(), args.toArray());
	}

	public boolean isUnique(Long id, String code, int type) {
		if (code == null)
			return false;

		Object[] args;
		String hql = "select id,code from BC_IDENTITY_ACTOR where type_ = ? and code = ?";
		if (id != null) {
			hql += " and id != ?";
			args = new Object[] { type, code, id };
		} else {
			args = new Object[] { type, code };
		}

		List<Object[]> o = HibernateJpaNativeQuery.executeNativeSql(
				getJpaTemplate(), hql, args, new RowMapper<Object[]>() {
					public Object[] mapRow(Object[] rs, int rowNum) {
						return rs;
					}
				});

		return o == null || o.isEmpty();
	}

	public String[] findMailAddressByGroup(List<String> groupCodes) {
		if (groupCodes == null || groupCodes.isEmpty())
			return null;
		ArrayList<Object> args = new ArrayList<Object>();
		StringBuffer hql = new StringBuffer();
		hql.append("select distinct email from (");
		hql.append("select u.email as email from bc_identity_actor u");
		hql.append(" inner join bc_identity_actor_relation r on r.follower_id=u.id");
		hql.append(" inner join bc_identity_actor g on g.id=r.master_id");
		hql.append(" where u.type_=" + Actor.TYPE_USER + " and r.type_="
				+ ActorRelation.TYPE_BELONG + " and g.type_="
				+ Actor.TYPE_GROUP);
		hql.append(" and u.email like '%@%'");
		if (groupCodes.size() == 1) {
			hql.append(" and g.code=?");
			args.add(groupCodes.get(0));
		} else {
			hql.append(" and g.code in (");
			for (int i = 0; i < groupCodes.size(); i++) {
				if (i == 0)
					hql.append("?");
				else
					hql.append(",?");
				args.add(groupCodes.get(i));
			}
			hql.append(")");
		}
		hql.append(" order by u.code");
		hql.append(") as emails");
		if (logger.isDebugEnabled()) {
			logger.debug("args="
					+ StringUtils.collectionToCommaDelimitedString(args)
					+ ";hql=" + hql.toString());
		}
		List<String> result = HibernateJpaNativeQuery.executeNativeSql(
				getJpaTemplate(), hql.toString(), args.toArray(), null);

		return result.toArray(new String[0]);
	}

	public String[] findMailAddressByUser(String[] userCodes) {
		if (userCodes == null || userCodes.length == 0)
			return null;
		ArrayList<Object> args = new ArrayList<Object>();
		StringBuffer hql = new StringBuffer();
		hql.append("select distinct email from (");
		hql.append("select u.email as email from bc_identity_actor u");
		hql.append(" where u.type_=" + Actor.TYPE_USER);
		hql.append(" and u.email like '%@%'");
		if (userCodes.length  == 1) {
			hql.append(" and u.code=?");
			args.add(userCodes[0]);
		} else {
			hql.append(" and u.code in (");
			for (int i = 0; i < userCodes.length ; i++) {
				if (i == 0)
					hql.append("?");
				else
					hql.append(",?");
				args.add(userCodes[i]);
			}
			hql.append(")");
		}
		hql.append(" order by u.code");
		hql.append(") as emails");
		if (logger.isDebugEnabled()) {
			logger.debug("args="
					+ StringUtils.collectionToCommaDelimitedString(args)
					+ ";hql=" + hql.toString());
		}
		List<String> result = HibernateJpaNativeQuery.executeNativeSql(
				getJpaTemplate(), hql.toString(), args.toArray(), null);

		return result.toArray(new String[0]);
	}
}
