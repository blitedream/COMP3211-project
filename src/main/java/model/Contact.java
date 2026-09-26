package model;

/** Contact PIR with name, address, and mobile number (US5). */
public final class Contact extends PIR {
    private final String name;
    private final String address;
    private final String mobileNumber;

    public Contact(String id, String name, String address, String mobileNumber) {
        super(id);
        this.name = requireText(name, "name");
        this.address = requireText(address, "address");
        this.mobileNumber = requireText(mobileNumber, "mobileNumber");
    }

    @Override
    public RecordType getType() {
        return RecordType.CONTACT;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }
}
