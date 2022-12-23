package contacts

import kotlinx.datetime.*

data class Property(val propertyName: String, val updater: (String) -> Unit, val valueOf: () -> String)

interface Menu {
    val name: String

    val subMenu: List<Menu>

    fun action(vararg contact: Contact): Boolean
}

abstract class Contact {
    abstract var phoneNumber: String
    abstract val creationDate: LocalDateTime
    abstract var editionDate: LocalDateTime

    protected abstract val properties: List<Property>

    private val phoneNumberRegex: Regex
        get() = "(\\+?(([A-Za-z0-9]+)|(\\([A-Za-z0-9]+\\))|(\\([A-Za-z0-9]+\\)[\\s-][A-Za-z0-9]{2,})|([A-Za-z0-9]+[\\s-]\\([A-Za-z0-9]{2,}\\))|([A-Za-z0-9]+[\\s-][A-Za-z0-9]{2,})))?(([A-Za-z0-9]{2,}([\\s-][A-Za-z0-9]{2,})*)|([\\s-][A-Za-z0-9]{2,})+)?".toRegex()

    fun isPhoneValid() = phoneNumberRegex.matches(phoneNumber)

    abstract fun validatePhoneInput(): Contact

    abstract fun info()

    fun updatableProperties(): Array<String> = properties.map(Property::propertyName).toTypedArray()

    fun updateProperty(entry: Pair<String, String>) =
            properties.first { it.propertyName == entry.first }.updater(entry.second)

    abstract fun searchablePresentation(): String
}

data class Person(
        var name: String,
        var surname: String,
        var birthdate: String,
        var gender: String,
        override var phoneNumber: String
) : Contact() {
    override val properties: List<Property>

    init {
        properties = listOf(
                Property(
                        propertyName = "name",
                        updater = {
                            name = it
                        },
                        valueOf = { name }
                ),
                Property(
                        propertyName = "surname",
                        updater = {
                            surname = it
                        },
                        valueOf = { surname }
                ),
                Property(
                        propertyName = "birth",
                        updater = {
                            birthdate = try {
                                it.also {
                                    it.toLocalDate()
                                }
                            } catch (ex: Exception) {
                                println("Bad birth date!")
                                ""
                            }
                        },
                        valueOf = { birthdate.takeIf { it.isNotEmpty() } ?: "[no data]" }
                ),
                Property(
                        propertyName = "gender",
                        updater = {
                            gender = it.run {
                                if (this in listOf("M", "F")) {
                                    this
                                } else {
                                    println("Bad gender!")
                                    ""
                                }
                            }
                        },
                        valueOf = { gender.takeIf { it.isNotEmpty() } ?: "[no data]" }
                ),
                Property(
                        propertyName = "number",
                        updater = { value ->
                            phoneNumber = (copy(phoneNumber = value).takeIf { it.isPhoneValid() }
                                    ?: copy(phoneNumber = "")
                                            .also {
                                                println("Wrong number format!")
                                            }).phoneNumber
                        },
                        valueOf = { phoneNumber.takeIf { it.isNotEmpty() } ?: "[no number]" }
                )
        )
    }

    override val creationDate: LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.UTC)
    override var editionDate: LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.UTC)

    override fun validatePhoneInput(): Person = takeIf { it.isPhoneValid() } ?: copy(phoneNumber = "").also {
        println("Wrong number format!")
    }

    override fun info() {
        println("""
            Name: $name
            Surname: $surname
            Birth date: ${birthdate.takeIf { it.isNotEmpty() } ?: "[no data]"}
            Gender: ${gender.takeIf { it.isNotEmpty() } ?: "[no data]"}
            Number: ${phoneNumber.takeIf { it.isNotEmpty() } ?: "[no number]"}
            Time created: $creationDate
            Time last edit: $editionDate
        """.trimIndent())
    }

    override fun searchablePresentation(): String = """
        ${component1()}
        ${component2()}
        ${component3()}
        ${component4()}
        ${component5()}
    """.trimIndent()

    override fun toString(): String {
        return "$name $surname"
    }
}

data class Organization(
        var name: String,
        var address: String,
        override var phoneNumber: String
) : Contact() {
    override val properties: List<Property>

    init {
        properties = listOf(
                Property(
                        propertyName = "name",
                        updater = {
                            name = it
                        },
                        valueOf = { name }
                ),
                Property(
                        propertyName = "address",
                        updater = {
                            address = it
                        },
                        valueOf = { address }
                ),
                Property(
                        propertyName = "number",
                        updater = { value ->
                            phoneNumber = (copy(phoneNumber = value).takeIf { it.isPhoneValid() }
                                    ?: copy(phoneNumber = "")
                                            .also {
                                                println("Wrong number format!")
                                            }).phoneNumber
                        },
                        valueOf = { phoneNumber.takeIf { it.isNotEmpty() } ?: "[no number]" }
                )
        )
    }

    override val creationDate: LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.UTC)
    override var editionDate: LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.UTC)

    override fun validatePhoneInput(): Organization = takeIf { it.isPhoneValid() } ?: copy(phoneNumber = "").also {
        println("Wrong number format!")
    }

    override fun info() {
        println("""
            Organization name: $name
            Address: $address
            Number: ${phoneNumber.takeIf { it.isNotEmpty() } ?: "[no number]"}
            Time created: $creationDate
            Time last edit: $editionDate
        """.trimIndent())
    }

    override fun toString(): String {
        return name
    }

    override fun searchablePresentation(): String = """
        ${component1()}
        ${component2()}
        ${component3()}
    """.trimIndent()
}

interface ContactApp {
    operator fun invoke()
}

object ContactAppImpl : ContactApp {

    private val contacts = mutableListOf<Contact>()

    private val add = object : Menu {
        override val name: String = "add"
        override val subMenu: List<Menu> = emptyList()
        override fun action(vararg contact: Contact): Boolean {
            addContact()
            println()
            return true
        }
    }

    private val count = object : Menu {
        override val name: String = "count"
        override val subMenu: List<Menu> = emptyList()
        override fun action(vararg contact: Contact): Boolean {
            println("The Phone Book has ${contacts.size} records.")
            println()
            return true
        }
    }
    private val number = object : Menu {
        private val delete = object : Menu {
            override val name: String = "delete"
            override val subMenu: List<Menu> = emptyList()

            override fun action(vararg contact: Contact): Boolean {
                contacts.removeAll(contact.toSet())
                println("The record removed!")
                println()
                return false
            }
        }

        private val edit = object : Menu {
            override val name: String = "edit"
            override val subMenu: List<Menu> = emptyList()

            override fun action(vararg contact: Contact): Boolean {
                val fieldToEdit = input("Select a field (${contact.first().updatableProperties().joinToString()}): ")
                val update = input("Enter $fieldToEdit: ")
                contact.first().apply {
                    updateProperty(fieldToEdit to update)
                    editionDate = Clock.System.now().toLocalDateTime(TimeZone.UTC)
                }
                println("The record updated!")
                return true
            }
        }

        private val menuAction = "menu"

        override val name: String = "[number]"
        override val subMenu: List<Menu> = listOf(edit, delete)

        override fun action(vararg contact: Contact): Boolean {
            contact.first().info()
            println()
            showSubMenu(contact.first())
            return false
        }

        private fun showSubMenu(contact: Contact) {
            val actions = subMenu.map(Menu::name).toTypedArray() + listOf(menuAction)
            val choice = menu("record", actions)
            if (choice == menuAction) {
                return
            }
            if (choice in actions) {
                val action = subMenu.first { it.name == choice }
                if (action.action(contact)) {
                    action(contact)
                }
            } else {
                println("Unknown action...")
                println()
                showSubMenu(contact)
            }
        }
    }

    private val search = object : Menu {
        private lateinit var result: List<Contact>
        private val againAction = "again"
        private val backAction = "back"
        override val name: String = "search"
        override val subMenu: List<Menu> = listOf(number)

        override fun action(vararg contact: Contact): Boolean {
            result = searchInContacts()
            if (result.isNotEmpty()) {
                println("Found ${result.size} results: ")
                result.listContacts()
            } else {
                println("Found 0 result")
            }
            println()
            showSubMenu()
            return true
        }

        private fun showSubMenu() {
            val actions = subMenu.map(Menu::name).toTypedArray() + listOf(backAction, againAction)
            val choice = menu(name, actions)
            if (choice in listOf(backAction, againAction)) {
                when (choice) {
                    againAction -> action()
                    backAction -> {}
                }
                return
            }
            try {
                val contactPosition = choice.toInt()
                if (number.action(result[contactPosition - 1])) {
                    action()
                }
            } catch (_: Exception) {
                println("Unknown action...")
                println()
                showSubMenu()
            }
        }
    }

    private val list = object : Menu {
        private val backAction = "back"
        override val name: String = "list"
        override val subMenu: List<Menu> = listOf(number)

        override fun action(vararg contact: Contact): Boolean {
            contacts.listContacts()
            println()
            showSubMenu()
            return true
        }

        private fun showSubMenu() {
            val actions = subMenu.map(Menu::name).toTypedArray() + listOf(backAction)
            val choice = menu(name, actions)
            if (choice in listOf(backAction)) {
                when (choice) {
                    backAction -> {}
                }
                return
            }
            try {
                val contactPosition = choice.toInt()
                if (number.action(contacts[contactPosition - 1])) {
                    action()
                }
            } catch (_: Exception) {
                println("Unknown action...")
                println()
                showSubMenu()
            }
        }
    }

    private val exit = object : Menu {
        override val name: String = "exit"
        override val subMenu: List<Menu> = emptyList()

        override fun action(vararg contact: Contact): Boolean {
            return false
        }
    }

    private val menu: List<Menu> = listOf(
            add,
            list,
            search,
            count,
            exit
    )

    private fun menu(menuName: String, actions: Array<String>): String =
            input("[$menuName] Enter action (${actions.joinToString()}): ")

    private fun searchInContacts(): List<Contact> {
        val query = input("Enter search query: ")
        return contacts.filter {
            it.searchablePresentation().contains(query, ignoreCase = true)
        }
    }

    private fun List<Contact>.listContacts() {
        forEachIndexed { index, contact ->
            println("${index + 1}. $contact")
        }
    }

    private fun input(title: String): String {
        println(title)
        return readln()
    }

    private fun addContact() {
        val newContact = when (input("Enter the type (person, organization): ")) {
            "person" -> {
                Person(
                        name = input("Enter the name of the person:"),
                        surname = input("Enter the surname of the person:"),
                        birthdate = try {
                            input("Enter the birth date: ").also {
                                it.toLocalDate()
                            }
                        } catch (ex: Exception) {
                            println("Bad birth date!")
                            ""
                        },
                        gender = input("Enter the gender (M, F): ").run {
                            if (this in listOf("M", "F")) {
                                this
                            } else {
                                println("Bad gender!")
                                ""
                            }
                        },
                        phoneNumber = input("Enter the number: ")
                )
            }

            else -> {
                Organization(
                        name = input("Enter the organization name: "),
                        address = input("Enter the address: "),
                        phoneNumber = input("Enter the number: ")
                )
            }
        }
        contacts.add(newContact.validatePhoneInput())
        println("The record added.")
    }

    override tailrec fun invoke() {
        val actions = menu.map(Menu::name).toTypedArray()
        val choice = menu("menu", actions)
        if (choice in actions) {
            val action = menu.first { it.name == choice }
            if (action.action()) {
                invoke()
            }
        } else {
            println("Unknown action...")
            println()
            invoke()
        }
    }
}

fun main() {
    ContactAppImpl()
}
