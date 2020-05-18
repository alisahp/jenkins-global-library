using System;

namespace DevOpsLeadsApi.Api
{
    public class Lead
    {
        public Lead() : this(null, null, null){}
        public Lead(string firstName, string lastName) : this(null, firstName, lastName){}
        
        public Lead(int? id, string firstName, string lastName)
        {
            Id = id;
            FirstName = firstName;
            LastName = lastName;
        }
        public int? Id { get; set; }
        
        public string FullName => $"{FirstName} {LastName}";
        public string FirstName { get; set; }
        public string LastName { get; set; }
        public string Address { get; set; }
        public string Service { get; set; }
        
        public DateTime CreatedAt { get; set; }
        
        public override bool Equals(object obj)
        {
            if (!(obj is Lead other))
            {
                return false;
            }
            return Id == other.Id;
        }
        
        public override int GetHashCode()
        {
            return Id.GetHashCode();
        }
        
        public Lead Clone() => new Lead
        {
            Id = this.Id,
            FirstName = this.FirstName,
            LastName = this.LastName,
            Address = this.Address,
            Service = this.Service,
            CreatedAt = this.CreatedAt
        };
    }
}