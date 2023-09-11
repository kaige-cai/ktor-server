package ok.model

@kotlinx.serialization.Serializable
data class User(val username: String, val password: String)

@kotlinx.serialization.Serializable
data class UserList(val users: MutableList<User>)
