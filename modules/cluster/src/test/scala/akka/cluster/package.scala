package akka

import akka.actor.Address

/**
  * Created by dev on 16/11/16.
  */
package object cluster {

  /**
    * This function let create instance of Member class which have akka.cluster private constructor
    * @param protocol protocol
    * @param system cluster's actor system name
    * @param uid unique id of cluster member instance
    * @param status status of cluster member
    * @return instance of Member class
    */
  def createTestClusterMember(protocol:String, system:String, uid:Long, status:MemberStatus):Member = {
    val address1: Address = Address(protocol, system)
    val uniqueAddress1: UniqueAddress = UniqueAddress(address1, uid)

    new Member(uniqueAddress1, 0, status, Set())
  }

}
