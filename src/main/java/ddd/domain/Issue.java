package ddd.domain;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.*;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import ddd.infrastructure.DateUserType;
import ddd.infrastructure.IssueNumberType;
import ddd.infrastructure.ParticipantIDType;
import ddd.infrastructure.ProductVersionType;
import org.springframework.util.Assert;

@Entity
@TypeDefs({
    @TypeDef(typeClass = IssueNumberType.class, defaultForType=IssueNumber.class),
    @TypeDef(typeClass = ProductVersionType.class, defaultForType=ProductVersion.class),
    @TypeDef(typeClass = ParticipantIDType.class, defaultForType=ParticipantID.class),
    @TypeDef(name = "dateUserType", typeClass = DateUserType.class, defaultForType=Date.class)
})
public class Issue {

    public enum Status {

        OPEN {
            protected Status assigned() { return ASSIGNED; }
            protected Status resolve() { return RESOLVED; }
        }, 
        ASSIGNED {
            protected Status assigned() { return this; }
            protected Status resolve() { return RESOLVED; }
        }, 
        RESOLVED {
            protected Status assigned() { return this; }
            protected Status resolve() { return this; }
            protected Status close() { return CLOSED; }
            protected Status reopen() { return OPEN; }
        }, 
        CLOSED {
            protected Status reopen() { return OPEN; }
        };

        private Status transitionNotAllowed(String action) {
            throw new IllegalStateException(String.format("Cannot %s already %s issue!", action, this));
        }
        
        protected Status assigned() {
            return transitionNotAllowed("assign");
        }

        protected Status resolve() {
            return transitionNotAllowed("resolve");
        }

        protected Status close() {
            return transitionNotAllowed("close");
        }

        protected Status reopen() {
            return transitionNotAllowed("reopen");
        }
    }

    public enum Resolution {
        FIXED, DUPLICATE, WONT_FIX, CANNOT_REPRODUCE
    }

    @Id
    private IssueNumber number;
    private String title;
    private String description;

    private Status status;
    private Resolution resolution;
    
    @Type(type="dateUserType")
    private Date createdAt;

    private ProductVersion occurredIn;
    private ProductVersion fixVersion;
    private ParticipantID assignee;

    @OneToMany(cascade=CascadeType.ALL)
    private Set<RelatedIssue> relatedIssues = new HashSet<>();
    
    protected Issue() {
        this.status = Status.OPEN;
    }

    public Issue(IssueNumber number, String title, ProductVersion occurredIn, Date createdAt) {
        this();
        this.number = number;
        this.title = title;
        this.occurredIn = occurredIn;
        this.createdAt = createdAt;
    }
    public IssueNumber number() {
        return number;
    }

    public ProductVersion occuredIn() {
        return occurredIn;
    }

    public void renameTo(String newTitle) {
        this.title = newTitle;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public Date createdAt() {
        return createdAt;
    }
    
    public void updateDescription(String newDescription){
        description = newDescription;
    }

    public Status status() {
        return status;
    }

    public ParticipantID assignee() {
        return assignee;
    }
    
    public void assignTo(ParticipantID assignee){
        
        Assert.isTrue(assignee != null, "Assignee cannot be null!");
        
        this.status = status.assigned();
        this.assignee = assignee;
    }

    private void resolveAs(Resolution solution) {
        
        this.status = status.resolve();
        this.resolution = solution;
    }
    
    public void fixedIn(ProductVersion version){

        Assert.isTrue(version != null, "Product version cannot be null!");
        
        resolveAs(Resolution.FIXED);
        this.fixVersion = version;
    }

    public void duplicateOf(IssueNumber duplicate){

        Assert.isTrue(duplicate != null, "Issue number cannot be null!");
        
        resolveAs(Resolution.DUPLICATE);
        this.relatedIssues.add(new RelatedIssue(duplicate, RelatedIssue.RelationshipType.DUPLICATES));
    }
    
    public void wontFix(String reason){

        Assert.isTrue(reason != null, "Wont Fix explenation cannot be empty!");
        Assert.isTrue(!reason.isEmpty(), "Wont Fix explenation cannot be empty!");
        
        resolveAs(Resolution.WONT_FIX);
    }

    public void cannotReproduce(){
        
        resolveAs(Resolution.CANNOT_REPRODUCE);
    }

    public Resolution resolution() {
        return resolution;
    }

    public ProductVersion fixVersion() {
        return fixVersion;
    }

    public boolean isDuplicateOf(IssueNumber issueNumber) {
        return hasRelationshipTo(issueNumber, RelatedIssue.RelationshipType.DUPLICATES);
    }

    public void close(){
        this.status = status.close();
    }
    public void reopen(ProductVersion version){
        
        Assert.isTrue(version != null, "Product Version cannot be null!");
        
        this.status = status.reopen();
        this.occurredIn = version;
        this.assignee = null;
    }

    public boolean hasRelationshipTo(IssueNumber issueNumber, RelatedIssue.RelationshipType type) {
        return relatedIssues.contains(new RelatedIssue(issueNumber, type));
    }

    public void blocks(IssueNumber issueNumber) {
        
        Assert.isTrue(issueNumber != null, "Issue number cannot be null!");
        
        this.relatedIssues.add(new RelatedIssue(issueNumber, RelatedIssue.RelationshipType.BLOCKS));
    }
    public void referTo(IssueNumber issueNumber) {
        
        Assert.isTrue(issueNumber != null, "Issue number cannot be null!");

        this.relatedIssues.add(new RelatedIssue(issueNumber, RelatedIssue.RelationshipType.REFERS_TO));
    }
    protected void isDuplicatedBy(IssueNumber duplicate){
        
        Assert.isTrue(assignee != null, "Issue number cannot be null!");
        
        this.relatedIssues.add(new RelatedIssue(duplicate, RelatedIssue.RelationshipType.IS_DUPLICATED_BY));
    }
    protected void isReferredBy(IssueNumber referee){
        
        Assert.isTrue(assignee != null, "Issue number cannot be null!");
        
        this.relatedIssues.add(new RelatedIssue(referee, RelatedIssue.RelationshipType.IS_REFERRED_BY));
    }
    protected void isBlockedBy(IssueNumber blocker){
        
        Assert.isTrue(assignee != null, "Issue number cannot be null!");
        
        this.relatedIssues.add(new RelatedIssue(blocker, RelatedIssue.RelationshipType.IS_BLOCKED_BY));
    }
}
